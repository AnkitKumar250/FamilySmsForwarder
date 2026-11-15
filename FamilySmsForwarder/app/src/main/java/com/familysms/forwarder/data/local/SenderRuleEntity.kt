package com.familysms.forwarder.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.familysms.forwarder.domain.MatchType

@Entity(tableName = "sender_rules")
data class SenderRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val senderPattern: String,
    val displayLabel: String = "",
    val matchType: MatchType = MatchType.EXACT,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
