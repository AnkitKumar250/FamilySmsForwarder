package com.familysms.forwarder.domain

object PhoneNumberNormalizer {

    private val PHONE_CANDIDATE_REGEX = Regex("^\\+?[0-9\\s\\-().]{5,}$")
    private val NON_PHONE_CHARS_REGEX = Regex("[^0-9+]")

    fun isPhoneNumberLike(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false
        return PHONE_CANDIDATE_REGEX.matches(trimmed) && trimmed.any { it.isDigit() }
    }

    /**
     * Normalizes a phone number by keeping only digits and an optional leading '+'.
     * For alphanumeric sender IDs (e.g. "VM-HDFCBK"), trims and uppercases.
     */
    fun normalizeSender(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        if (isPhoneNumberLike(trimmed)) {
            val cleaned = NON_PHONE_CHARS_REGEX.replace(trimmed, "")
            return if (cleaned.startsWith("+")) {
                "+" + cleaned.drop(1).replace("+", "")
            } else {
                cleaned.replace("+", "")
            }
        }
        return trimmed.uppercase()
    }

    fun digitsOnly(input: String): String {
        return input.filter { it.isDigit() }
    }

    /**
     * Compares two phone numbers tolerating country-code or leading-zero differences
     * when both numbers contain at least 10 digits.
     */
    fun phoneNumbersMatch(a: String, b: String): Boolean {
        if (!isPhoneNumberLike(a) || !isPhoneNumberLike(b)) return false
        val normA = normalizeSender(a)
        val normB = normalizeSender(b)
        if (normA == normB) return true

        val digitsA = digitsOnly(normA).trimStart('0')
        val digitsB = digitsOnly(normB).trimStart('0')
        if (digitsA.isEmpty() || digitsB.isEmpty()) return false
        if (digitsA == digitsB) return true

        // Compare trailing 10 digits when one includes a country code and the other is local
        if (digitsA.length >= 10 && digitsB.length >= 10) {
            return digitsA.takeLast(10) == digitsB.takeLast(10)
        }
        return false
    }

    fun isValidDestinationNumber(input: String): Boolean {
        val normalized = normalizeSender(input)
        if (!isPhoneNumberLike(normalized)) return false
        val digits = digitsOnly(normalized)
        return digits.length in 7..15
    }
}
