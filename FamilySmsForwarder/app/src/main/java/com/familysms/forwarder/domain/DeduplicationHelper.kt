package com.familysms.forwarder.domain

import java.security.MessageDigest

object DeduplicationHelper {

    fun buildDedupeKey(
        senderAddress: String,
        messageBody: String,
        timestampMillis: Long,
        windowSeconds: Int
    ): String {
        val normalizedSender = PhoneNumberNormalizer.normalizeSender(senderAddress)
        val normalizedBody = messageBody.trim()
        val safeWindowMs = (windowSeconds.coerceAtLeast(1)) * 1000L
        val bucket = timestampMillis / safeWindowMs
        val payload = "$normalizedSender|$normalizedBody|$bucket"

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))

        return digest.joinToString("") { "%02x".format(it) }
    }
}
