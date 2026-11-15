package com.familysms.forwarder.data.local

import androidx.room.TypeConverter
import com.familysms.forwarder.domain.ForwardStatus
import com.familysms.forwarder.domain.MatchType

class Converters {

    @TypeConverter
    fun fromMatchType(value: MatchType): String = value.name

    @TypeConverter
    fun toMatchType(value: String): MatchType =
        runCatching { MatchType.valueOf(value) }.getOrDefault(MatchType.EXACT)

    @TypeConverter
    fun fromForwardStatus(value: ForwardStatus): String = value.name

    @TypeConverter
    fun toForwardStatus(value: String): ForwardStatus =
        runCatching { ForwardStatus.valueOf(value) }.getOrDefault(ForwardStatus.QUEUED)
}
