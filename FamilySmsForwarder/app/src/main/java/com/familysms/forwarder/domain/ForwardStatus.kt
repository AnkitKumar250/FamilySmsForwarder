package com.familysms.forwarder.domain

enum class ForwardStatus(val label: String) {
    QUEUED("Queued"),
    SENDING("Sending"),
    SENT("Sent"),
    DELIVERED("Delivered"),
    FAILED("Failed"),
    SKIPPED_DUPLICATE("Duplicate Skipped")
}
