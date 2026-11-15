package com.familysms.forwarder.domain

import com.familysms.forwarder.data.local.SenderRuleEntity

object SenderMatcher {

    fun findMatchingRule(
        incomingSender: String,
        rules: List<SenderRuleEntity>
    ): SenderRuleEntity? {
        if (incomingSender.isBlank()) return null
        return rules.firstOrNull { rule ->
            rule.isEnabled && matches(incomingSender, rule.senderPattern, rule.matchType)
        }
    }

    fun matches(
        incomingSender: String,
        rulePattern: String,
        matchType: MatchType
    ): Boolean {
        val incomingTrimmed = incomingSender.trim()
        val patternTrimmed = rulePattern.trim()
        if (incomingTrimmed.isEmpty() || patternTrimmed.isEmpty()) return false

        return when (matchType) {
            MatchType.EXACT -> {
                if (PhoneNumberNormalizer.isPhoneNumberLike(incomingTrimmed) &&
                    PhoneNumberNormalizer.isPhoneNumberLike(patternTrimmed)
                ) {
                    PhoneNumberNormalizer.phoneNumbersMatch(incomingTrimmed, patternTrimmed)
                } else {
                    PhoneNumberNormalizer.normalizeSender(incomingTrimmed) ==
                        PhoneNumberNormalizer.normalizeSender(patternTrimmed)
                }
            }

            MatchType.CONTAINS -> {
                val normIncoming = PhoneNumberNormalizer.normalizeSender(incomingTrimmed)
                val normPattern = PhoneNumberNormalizer.normalizeSender(patternTrimmed)
                normIncoming.contains(normPattern, ignoreCase = true) ||
                    incomingTrimmed.contains(patternTrimmed, ignoreCase = true)
            }

            MatchType.STARTS_WITH -> {
                val normIncoming = PhoneNumberNormalizer.normalizeSender(incomingTrimmed)
                val normPattern = PhoneNumberNormalizer.normalizeSender(patternTrimmed)
                normIncoming.startsWith(normPattern, ignoreCase = true) ||
                    incomingTrimmed.startsWith(patternTrimmed, ignoreCase = true)
            }
        }
    }
}
