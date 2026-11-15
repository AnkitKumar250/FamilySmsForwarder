package com.familysms.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SubscriptionManager
import com.familysms.forwarder.FamilySmsApplication
import com.familysms.forwarder.data.repository.IncomingEvaluationResult
import com.familysms.forwarder.worker.ForwardSmsWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val subscriptionId = extractSubscriptionId(intent)

        // Group multipart PDU segments by originating address while preserving segment order
        val groupedBodies = LinkedHashMap<String, StringBuilder>()
        val groupedTimestamps = HashMap<String, Long>()

        for (sms in messages) {
            val sender = sms.originatingAddress
                ?: sms.displayOriginatingAddress
                ?: continue
            val bodyPart = sms.messageBody ?: ""
            val timestamp = sms.timestampMillis.takeIf { it > 0L } ?: System.currentTimeMillis()

            groupedBodies.getOrPut(sender) { StringBuilder() }.append(bodyPart)
            groupedTimestamps.putIfAbsent(sender, timestamp)
        }

        if (groupedBodies.isEmpty()) return

        val pendingResult = goAsync()
        val app = context.applicationContext as FamilySmsApplication

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                for ((sender, bodyBuilder) in groupedBodies) {
                    val fullBody = bodyBuilder.toString()
                    val timestamp = groupedTimestamps[sender] ?: System.currentTimeMillis()
                    when (
                        val result = app.repository.evaluateIncomingSms(
                            senderAddress = sender,
                            messageBody = fullBody,
                            receivedTimestamp = timestamp
                        )
                    ) {
                        is IncomingEvaluationResult.EnqueueForward -> {
                            ForwardSmsWorker.enqueue(
                                context = app,
                                logId = result.logId,
                                subscriptionId = subscriptionId
                            )
                        }
                        is IncomingEvaluationResult.DuplicateSkipped,
                        is IncomingEvaluationResult.FailedPrecondition,
                        IncomingEvaluationResult.Ignored -> Unit
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun extractSubscriptionId(intent: Intent): Int {
        val fromExtra = intent.getIntExtra(
            "subscription",
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        )
        if (fromExtra != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return fromExtra
        }
        return intent.getIntExtra(
            SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        )
    }
}
