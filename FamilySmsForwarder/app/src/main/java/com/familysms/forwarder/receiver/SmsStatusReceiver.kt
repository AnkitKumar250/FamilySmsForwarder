package com.familysms.forwarder.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.familysms.forwarder.FamilySmsApplication
import com.familysms.forwarder.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val logId = intent.getLongExtra(EXTRA_LOG_ID, -1L)
        if (logId <= 0L) return

        val currentResultCode = resultCode
        val errorCodeExtra = intent.getIntExtra("errorCode", -1)
        val pendingResult = goAsync()
        val app = context.applicationContext as FamilySmsApplication

        serialScope.launch {
            try {
                val settings = app.settingsRepository.getCurrentSettings()
                val logEntry = app.repository.getLogById(logId)

                when (action) {
                    ACTION_SMS_SENT -> {
                        val success = currentResultCode == Activity.RESULT_OK
                        val errorMsg = if (success) null else mapSmsErrorCode(currentResultCode, errorCodeExtra)
                        app.repository.recordPartSentResult(
                            logId = logId,
                            success = success,
                            errorMessage = errorMsg
                        )
                        if (!success && settings.notifyOnError && logEntry != null) {
                            NotificationHelper.showStatusNotification(
                                context = app,
                                notificationId = logId.toInt(),
                                title = "SMS Send Failed",
                                body = "Forwarding from ${logEntry.originalSender} failed: $errorMsg"
                            )
                        } else if (success && settings.notifyOnForwarded && logEntry != null) {
                            val updated = app.repository.getLogById(logId)
                            if (updated != null && updated.sentPartsCount >= updated.totalParts) {
                                NotificationHelper.showStatusNotification(
                                    context = app,
                                    notificationId = logId.toInt(),
                                    title = "SMS Forwarded",
                                    body = "Forwarded SMS from ${updated.originalSender} to ${updated.destinationNumber}."
                                )
                            }
                        }
                    }

                    ACTION_SMS_DELIVERED -> {
                        val success = currentResultCode == Activity.RESULT_OK
                        val errorMsg = if (success) null else "Delivery report failed (code $currentResultCode)"
                        app.repository.recordPartDeliveredResult(
                            logId = logId,
                            success = success,
                            errorMessage = errorMsg
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun mapSmsErrorCode(resultCode: Int, errorCodeExtra: Int): String {
        val base = when (resultCode) {
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Carrier generic failure"
            SmsManager.RESULT_ERROR_RADIO_OFF -> "Cellular radio is turned off (Airplane mode)"
            SmsManager.RESULT_ERROR_NULL_PDU -> "Null PDU provided to SmsManager"
            SmsManager.RESULT_ERROR_NO_SERVICE -> "No cellular service available"
            SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "Carrier or OS SMS rate limit exceeded"
            SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE -> "Blocked by Fixed Dialing Number (FDN)"
            SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED -> "Short code / premium SMS not allowed"
            SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED -> "Short code permanently blocked by user"
            else -> "SMS send failed (resultCode=$resultCode)"
        }
        return if (errorCodeExtra >= 0) "$base [errorCode=$errorCodeExtra]" else base
    }

    companion object {
        const val ACTION_SMS_SENT = "com.familysms.forwarder.ACTION_SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.familysms.forwarder.ACTION_SMS_DELIVERED"
        const val EXTRA_LOG_ID = "extra_log_id"
        const val EXTRA_PART_INDEX = "extra_part_index"
        const val EXTRA_TOTAL_PARTS = "extra_total_parts"

        @OptIn(ExperimentalCoroutinesApi::class)
        private val serialDispatcher = Dispatchers.IO.limitedParallelism(1)
        private val serialScope = CoroutineScope(SupervisorJob() + serialDispatcher)
    }
}
