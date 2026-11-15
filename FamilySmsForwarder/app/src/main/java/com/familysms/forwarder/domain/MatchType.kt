package com.familysms.forwarder.domain

enum class MatchType(
    val displayName: String,
    val description: String
) {
    EXACT(
        displayName = "Exact / Phone Match",
        description = "Matches normalized phone numbers (ignoring spaces, dashes, or country prefix) or exact sender IDs."
    ),
    CONTAINS(
        displayName = "Contains",
        description = "Ideal for carrier-prefixed sender IDs (e.g., HDFCBK matches VM-HDFCBK or AD-HDFCBK)."
    ),
    STARTS_WITH(
        displayName = "Starts With",
        description = "Matches any sender ID or number starting with the specified text."
    )
}
