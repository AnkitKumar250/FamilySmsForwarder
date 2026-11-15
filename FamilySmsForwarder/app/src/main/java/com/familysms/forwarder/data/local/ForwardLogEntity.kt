package com.familysms.forwarder.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familysms.forwarder.domain.ForwardStatus

@Entity(
    tableName = "forward_logs",
    indices = [
        Index(value = ["dedupeKey"]),
        Index(value = ["receivedAt"])
    ]
)
data class ForwardLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val originalSender: String,
    val matchedRuleLabel: String,
    val destinationNumber: String,
    val messageBody: String,
    val forwardedContent: String,
    val dedupeKey: String,
    val status: ForwardStatus,
    val totalParts: Int = 1,
    val sentPartsCount: Int = 0,
    val deliveredPartsCount: Int = 0,
    val errorMessage: String? = null,
    val receivedAt: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
