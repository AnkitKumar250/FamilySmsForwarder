package com.familysms.forwarder.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.familysms.forwarder.FamilySmsApplication
import com.familysms.forwarder.domain.ForwardStatus
import com.familysms.forwarder.receiver.SmsStatusReceiver
import com.familysms.forwarder.util.NotificationHelper
import com.familysms.forwarder.util.PermissionHelper
import java.util.concurrent.TimeUnit

class ForwardSmsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return NotificationHelper.buildWorkerForegroundInfo(applicationContext)
    }

    override suspend fun doWork(): Result {
        val logId = inputData.getLong(KEY_LOG_ID, -1L)
        if (logId <= 0L) return Result.failure()

        val subscriptionId = inputData.getInt(
            KEY_SUBSCRIPTION_ID,
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        )

        val app = applicationContext as FamilySmsApplication
        val repository = app.repository
        val settings = app.settingsRepository.getCurrentSettings()

        val logEntry = repository.getLogById(logId) ?: return Result.failure()

        if (logEntry.status != ForwardStatus.QUEUED) {
            return Result.success()
        }

        if (!PermissionHelper.isGranted(applicationContext, Manifest.permission.SEND_SMS)) {
            val errorMsg = "SEND_SMS permission is not granted."
            repository.markDispatchState(
                logId = logId,
                status = ForwardStatus.FAILED,
                totalParts = 1,
                errorMessage = errorMsg
            )
            if (settings.notifyOnError) {
                NotificationHelper.showStatusNotification(
                    context = applicationContext,
                    notificationId = logId.toInt(),
                    title = "SMS Forwarding Failed",
                    body = "Could not forward message from ${logEntry.originalSender}: $errorMsg"
                )
            }
            return Result.failure()
        }

        val smsManager = resolveSmsManager(applicationContext, subscriptionId)
        if (smsManager == null) {
            val errorMsg = "SmsManager service is unavailable on this device."
            repository.markDispatchState(
                logId = logId,
                status = ForwardStatus.FAILED,
                totalParts = 1,
                errorMessage = errorMsg
            )
            return Result.failure()
        }

        return try {
            val parts: ArrayList<String> = smsManager.divideMessage(logEntry.forwardedContent)
            val totalParts = parts.size.coerceAtLeast(1)

            repository.markDispatchState(
                logId = logId,
                status = ForwardStatus.SENDING,
                totalParts = totalParts,
                errorMessage = null
            )

            val sentIntents = ArrayList<PendingIntent>(totalParts)
            val deliveryIntents = ArrayList<PendingIntent>(totalParts)

            for (partIndex in 0 until totalParts) {
                sentIntents.add(
                    createStatusPendingIntent(
                        context = applicationContext,
                        action = SmsStatusReceiver.ACTION_SMS_SENT,
                        logId = logId,
                        partIndex = partIndex,
                        totalParts = totalParts,
                        requestCodeOffset = 0
                    )
                )
                deliveryIntents.add(
                    createStatusPendingIntent(
                        context = applicationContext,
                        action = SmsStatusReceiver.ACTION_SMS_DELIVERED,
                        logId = logId,
                        partIndex = partIndex,
                        totalParts = totalParts,
                        requestCodeOffset = 10_000
                    )
                )
            }

            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(
                    logEntry.destinationNumber,
                    null,
                    parts,
                    sentIntents,
                    deliveryIntents
                )
            } else {
                smsManager.sendTextMessage(
                    logEntry.destinationNumber,
                    null,
                    parts.firstOrNull() ?: logEntry.forwardedContent,
                    sentIntents.first(),
                    deliveryIntents.first()
                )
            }

            Result.success()
        } catch (sec: SecurityException) {
            val errorMsg = "SecurityException: ${sec.message ?: "SEND_SMS permission denied"}"
            repository.markDispatchState(
                logId = logId,
                status = ForwardStatus.FAILED,
                totalParts = 1,
                errorMessage = errorMsg
            )
            if (settings.notifyOnError) {
                NotificationHelper.showStatusNotification(
                    context = applicationContext,
                    notificationId = logId.toInt(),
                    title = "SMS Forwarding Blocked",
                    body = errorMsg
                )
            }
            Result.failure()
        } catch (iae: IllegalArgumentException) {
            val errorMsg = "Invalid destination or message payload: ${iae.message}"
            repository.markDispatchState(
                logId = logId,
                status = ForwardStatus.FAILED,
                totalParts = 1,
                errorMessage = errorMsg
            )
            Result.failure()
        } catch (ex: Exception) {
            val errorMsg = ex.message ?: "Unexpected error dispatching SMS"
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                repository.markDispatchState(
                    logId = logId,
                    status = ForwardStatus.FAILED,
                    totalParts = 1,
                    errorMessage = errorMsg
                )
                if (settings.notifyOnError) {
                    NotificationHelper.showStatusNotification(
                        context = applicationContext,
                        notificationId = logId.toInt(),
                        title = "SMS Forwarding Failed",
                        body = "Failed to forward SMS from ${logEntry.originalSender}: $errorMsg"
                    )
                }
                Result.failure()
            }
        }
    }

    private fun resolveSmsManager(context: Context, subscriptionId: Int): SmsManager? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val base = context.getSystemService(SmsManager::class.java)
            if (subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID && base != null) {
                runCatching { base.createForSubscriptionId(subscriptionId) }.getOrDefault(base)
            } else {
                base
            }
        } else {
            @Suppress("DEPRECATION")
            if (subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                runCatching { SmsManager.getSmsManagerForSubscriptionId(subscriptionId) }
                    .getOrElse { SmsManager.getDefault() }
            } else {
                SmsManager.getDefault()
            }
        }
    }

    private fun createStatusPendingIntent(
        context: Context,
        action: String,
        logId: Long,
        partIndex: Int,
        totalParts: Int,
        requestCodeOffset: Int
    ): PendingIntent {
        // Explicit Intent + FLAG_MUTABLE is required on Android 14+ so Telephony can populate resultCode/errorCode
        val intent = Intent(context, SmsStatusReceiver::class.java).apply {
            this.action = action
            setPackage(context.packageName)
            putExtra(SmsStatusReceiver.EXTRA_LOG_ID, logId)
            putExtra(SmsStatusReceiver.EXTRA_PART_INDEX, partIndex)
            putExtra(SmsStatusReceiver.EXTRA_TOTAL_PARTS, totalParts)
        }
        val requestCode = ((logId and 0xFFFF) * 100 + partIndex + requestCodeOffset).toInt()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    companion object {
        private const val KEY_LOG_ID = "log_id"
        private const val KEY_SUBSCRIPTION_ID = "subscription_id"

        fun enqueue(
            context: Context,
            logId: Long,
            subscriptionId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID
        ) {
            val input = Data.Builder()
                .putLong(KEY_LOG_ID, logId)
                .putInt(KEY_SUBSCRIPTION_ID, subscriptionId)
                .build()

            val request = OneTimeWorkRequestBuilder<ForwardSmsWorker>()
                .setInputData(input)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15L, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "forward_sms_$logId",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
