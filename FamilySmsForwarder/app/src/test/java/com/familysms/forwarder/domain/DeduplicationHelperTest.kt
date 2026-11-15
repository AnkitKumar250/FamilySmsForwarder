package com.familysms.forwarder.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DeduplicationHelperTest {

    @Test
    fun `identical messages within same window bucket produce identical dedupe keys`() {
        val baseTime = 1_700_000_040_000L
        val key1 = DeduplicationHelper.buildDedupeKey(
            senderAddress = "+1 555-000-1111",
            messageBody = "Your OTP is 482910",
            timestampMillis = baseTime,
            windowSeconds = 60
        )
        val key2 = DeduplicationHelper.buildDedupeKey(
            senderAddress = "+15550001111",
            messageBody = "  Your OTP is 482910  ",
            timestampMillis = baseTime + 5_000L,
            windowSeconds = 60
        )
        assertEquals(key1, key2)
    }

    @Test
    fun `different message bodies produce distinct dedupe keys`() {
        val now = 1_700_000_000_000L
        val key1 = DeduplicationHelper.buildDedupeKey(
            senderAddress = "VM-HDFCBK",
            messageBody = "Txn 100 INR",
            timestampMillis = now,
            windowSeconds = 60
        )
        val key2 = DeduplicationHelper.buildDedupeKey(
            senderAddress = "VM-HDFCBK",
            messageBody = "Txn 200 INR",
            timestampMillis = now,
            windowSeconds = 60
        )
        assertNotEquals(key1, key2)
    }
}
