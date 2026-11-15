package com.familysms.forwarder.domain

import com.familysms.forwarder.data.local.SenderRuleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SenderMatcherTest {

    @Test
    fun `phone numbers match across E164 and local 10-digit formats`() {
        assertTrue(
            SenderMatcher.matches(
                incomingSender = "+1 555-234-5678",
                rulePattern = "5552345678",
                matchType = MatchType.EXACT
            )
        )
        assertTrue(
            SenderMatcher.matches(
                incomingSender = "+919876543210",
                rulePattern = "098765 43210",
                matchType = MatchType.EXACT
            )
        )
    }

    @Test
    fun `alphanumeric sender IDs support contains and exact matching`() {
        assertTrue(
            SenderMatcher.matches(
                incomingSender = "VM-HDFCBK",
                rulePattern = "HDFCBK",
                matchType = MatchType.CONTAINS
            )
        )
        assertFalse(
            SenderMatcher.matches(
                incomingSender = "VM-HDFCBK",
                rulePattern = "HDFCBK",
                matchType = MatchType.EXACT
            )
        )
        assertTrue(
            SenderMatcher.matches(
                incomingSender = "vm-hdfcbk",
                rulePattern = "VM-HDFCBK",
                matchType = MatchType.EXACT
            )
        )
    }

    @Test
    fun `disabled rules are ignored by findMatchingRule`() {
        val rules = listOf(
            SenderRuleEntity(
                id = 1L,
                senderPattern = "HDFCBK",
                matchType = MatchType.CONTAINS,
                isEnabled = false
            ),
            SenderRuleEntity(
                id = 2L,
                senderPattern = "AIRTEL",
                matchType = MatchType.CONTAINS,
                isEnabled = true
            )
        )

        assertNull(SenderMatcher.findMatchingRule("VM-HDFCBK", rules))
        val matched = SenderMatcher.findMatchingRule("AD-AIRTEL", rules)
        assertNotNull(matched)
        assertEquals(2L, matched?.id)
    }
}
