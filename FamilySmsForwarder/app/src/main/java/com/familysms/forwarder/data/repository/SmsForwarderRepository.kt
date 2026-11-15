package com.familysms.forwarder.data.repository

import com.familysms.forwarder.data.local.AppDatabase
import com.familysms.forwarder.data.local.ForwardLogEntity
import com.familysms.forwarder.data.local.SenderRuleEntity
import com.familysms.forwarder.data.preferences.AppSettings
import com.familysms.forwarder.data.preferences.SettingsRepository
import com.familysms.forwarder.domain.DeduplicationHelper
import com.familysms.forwarder.domain.ForwardStatus
import com.familysms.forwarder.domain.MatchType
import com.familysms.forwarder.domain.PhoneNumberNormalizer
import com.familysms.forwarder.domain.SenderMatcher
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface IncomingEvaluationResult {
    data class EnqueueForward(val logId: Long, val destinationNumber: String) : IncomingEvaluationResult
    data object Ignored : IncomingEvaluationResult
    data class DuplicateSkipped(val logId: Long) : IncomingEvaluationResult
    data class FailedPrecondition(val reason: String) : IncomingEvaluationResult
}

class SmsForwarderRepository(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val senderRuleDao = database.senderRuleDao()
    private val forwardLogDao = database.forwardLogDao()

    val senderRulesFlow: Flow<List<SenderRuleEntity>> = senderRuleDao.observeAllRules()
    val recentLogsFlow: Flow<List<ForwardLogEntity>> = forwardLogDao.observeRecentLogs(100)
    val recentErrorsFlow: Flow<List<ForwardLogEntity>> = forwardLogDao.observeRecentErrors(20)
    val settingsFlow: Flow<AppSettings> = settingsRepository.settingsFlow

    suspend fun addSenderRule(
        senderPattern: String,
        displayLabel: String,
        matchType: MatchType
    ): Long {
        val cleanedPattern = senderPattern.trim()
        require(cleanedPattern.isNotEmpty()) { "Sender ID or phone number cannot be empty" }
        return senderRuleDao.insertRule(
            SenderRuleEntity(
                senderPattern = cleanedPattern,
                displayLabel = displayLabel.trim(),
                matchType = matchType,
                isEnabled = true
            )
        )
    }

    suspend fun setSenderRuleEnabled(ruleId: Long, enabled: Boolean) {
        senderRuleDao.setRuleEnabled(ruleId, enabled)
    }

    suspend fun deleteSenderRule(rule: SenderRuleEntity) {
        senderRuleDao.deleteRule(rule)
    }

    suspend fun clearLogs() {
        forwardLogDao.clearAllLogs()
    }

    suspend fun deleteLog(logId: Long) {
        forwardLogDao.deleteLogById(logId)
    }

    suspend fun evaluateIncomingSms(
        senderAddress: String,
        messageBody: String,
        receivedTimestamp: Long
    ): IncomingEvaluationResult {
        val settings = settingsRepository.getCurrentSettings()
        if (!settings.isMasterEnabled) {
            return IncomingEvaluationResult.Ignored
        }

        // Prevent infinite forwarding loops on already-forwarded messages
        if (messageBody.startsWith(FORWARD_HEADER_PREFIX)) {
            return IncomingEvaluationResult.Ignored
        }

        val enabledRules = senderRuleDao.getEnabledRules()
        val matchedRule = SenderMatcher.findMatchingRule(senderAddress, enabledRules)
            ?: return IncomingEvaluationResult.Ignored

        val destination = settings.destinationNumber.trim()
        if (!PhoneNumberNormalizer.isValidDestinationNumber(destination)) {
            forwardLogDao.insertLog(
                ForwardLogEntity(
                    originalSender = senderAddress,
                    matchedRuleLabel = matchedRule.displayLabel.ifBlank { matchedRule.senderPattern },
                    destinationNumber = destination.ifBlank { "Not configured" },
                    messageBody = messageBody,
                    forwardedContent = "",
                    dedupeKey = "",
                    status = ForwardStatus.FAILED,
                    errorMessage = "Destination mobile number is missing or invalid in Settings.",
                    receivedAt = receivedTimestamp
                )
            )
            return IncomingEvaluationResult.FailedPrecondition("Destination number missing or invalid")
        }

        // Prevent forwarding back to the incoming sender if user accidentally matches destination
        if (PhoneNumberNormalizer.phoneNumbersMatch(senderAddress, destination)) {
            return IncomingEvaluationResult.Ignored
        }

        val dedupeKey = DeduplicationHelper.buildDedupeKey(
            senderAddress = senderAddress,
            messageBody = messageBody,
            timestampMillis = receivedTimestamp,
            windowSeconds = settings.dedupeWindowSeconds
        )
        val sinceTimestamp = receivedTimestamp - (settings.dedupeWindowSeconds * 1000L)
        val existingCount = forwardLogDao.countActiveDuplicates(dedupeKey, sinceTimestamp)

        val formattedContent = formatForwardedMessage(
            originalSender = senderAddress,
            messageBody = messageBody,
            receivedTimestamp = receivedTimestamp,
            includeTimestamp = settings.includeTimestamp
        )

        if (existingCount > 0) {
            val skippedId = forwardLogDao.insertLog(
                ForwardLogEntity(
                    originalSender = senderAddress,
                    matchedRuleLabel = matchedRule.displayLabel.ifBlank { matchedRule.senderPattern },
                    destinationNumber = destination,
                    messageBody = messageBody,
                    forwardedContent = formattedContent,
                    dedupeKey = dedupeKey,
                    status = ForwardStatus.SKIPPED_DUPLICATE,
                    errorMessage = "Duplicate SMS suppressed within ${settings.dedupeWindowSeconds}s protection window.",
                    receivedAt = receivedTimestamp
                )
            )
            return IncomingEvaluationResult.DuplicateSkipped(skippedId)
        }

        val logId = forwardLogDao.insertLog(
            ForwardLogEntity(
                originalSender = senderAddress,
                matchedRuleLabel = matchedRule.displayLabel.ifBlank { matchedRule.senderPattern },
                destinationNumber = destination,
                messageBody = messageBody,
                forwardedContent = formattedContent,
                dedupeKey = dedupeKey,
                status = ForwardStatus.QUEUED,
                receivedAt = receivedTimestamp
            )
        )

        return IncomingEvaluationResult.EnqueueForward(
            logId = logId,
            destinationNumber = destination
        )
    }

    suspend fun getLogById(logId: Long): ForwardLogEntity? = forwardLogDao.getLogById(logId)

    suspend fun getQueuedLogs(): List<ForwardLogEntity> = forwardLogDao.getQueuedLogs()

    suspend fun markDispatchState(
        logId: Long,
        status: ForwardStatus,
        totalParts: Int,
        errorMessage: String? = null
    ) {
        forwardLogDao.updateDispatchState(
            logId = logId,
            status = status,
            totalParts = totalParts,
            errorMessage = errorMessage
        )
    }

    suspend fun recordPartSentResult(
        logId: Long,
        success: Boolean,
        errorMessage: String?
    ) {
        val current = forwardLogDao.getLogById(logId) ?: return
        if (current.status == ForwardStatus.FAILED) return

        if (!success) {
            forwardLogDao.updatePartProgress(
                logId = logId,
                status = ForwardStatus.FAILED,
                sentPartsCount = current.sentPartsCount,
                deliveredPartsCount = current.deliveredPartsCount,
                errorMessage = errorMessage ?: "Carrier rejected SMS transmission."
            )
            return
        }

        val newSent = (current.sentPartsCount + 1).coerceAtMost(current.totalParts)
        val newStatus = when {
            current.deliveredPartsCount >= current.totalParts -> ForwardStatus.DELIVERED
            newSent >= current.totalParts -> ForwardStatus.SENT
            else -> ForwardStatus.SENDING
        }
        forwardLogDao.updatePartProgress(
            logId = logId,
            status = newStatus,
            sentPartsCount = newSent,
            deliveredPartsCount = current.deliveredPartsCount,
            errorMessage = null
        )
    }

    suspend fun recordPartDeliveredResult(
        logId: Long,
        success: Boolean,
        errorMessage: String?
    ) {
        val current = forwardLogDao.getLogById(logId) ?: return
        if (current.status == ForwardStatus.FAILED) return

        if (!success) {
            forwardLogDao.updatePartProgress(
                logId = logId,
                status = ForwardStatus.FAILED,
                sentPartsCount = current.sentPartsCount,
                deliveredPartsCount = current.deliveredPartsCount,
                errorMessage = errorMessage ?: "SMS delivery report failed."
            )
            return
        }

        val newDelivered = (current.deliveredPartsCount + 1).coerceAtMost(current.totalParts)
        val newStatus = if (newDelivered >= current.totalParts) {
            ForwardStatus.DELIVERED
        } else {
            current.status
        }
        forwardLogDao.updatePartProgress(
            logId = logId,
            status = newStatus,
            sentPartsCount = current.sentPartsCount.coerceAtLeast(newDelivered),
            deliveredPartsCount = newDelivered,
            errorMessage = null
        )
    }

    private fun formatForwardedMessage(
        originalSender: String,
        messageBody: String,
        receivedTimestamp: Long,
        includeTimestamp: Boolean
    ): String {
        val header = if (includeTimestamp) {
            val timeStr = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
                .format(Date(receivedTimestamp))
            "${FORWARD_HEADER_PREFIX}$originalSender @ $timeStr]"
        } else {
            "${FORWARD_HEADER_PREFIX}$originalSender]"
        }
        return "$header\n$messageBody"
    }

    companion object {
        const val FORWARD_HEADER_PREFIX = "[Fwd from "
    }
}
