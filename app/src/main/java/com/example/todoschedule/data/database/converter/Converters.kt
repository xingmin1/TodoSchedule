package com.example.todoschedule.data.database.converter

import androidx.room.TypeConverter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/** Room类型转换器 */
class Converters {
    /** 整数列表转字符串 */
    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return value?.joinToString(",") ?: ""
    }

    /** 字符串转整数列表 */
    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        return value?.split(",")?.mapNotNull { if (it.isNotEmpty()) it.toIntOrNull() else null }
            ?: emptyList()
    }
    
    /** kotlinx.datetime.LocalDate 转为字符串 */
    @TypeConverter
    fun localDateToString(localDate: LocalDate?): String? {
        return localDate?.toString()
    }
    
    /** 字符串转为 kotlinx.datetime.LocalDate */
    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }
    
    /** kotlinx.datetime.LocalTime 转为字符串 */
    @TypeConverter
    fun localTimeToString(localTime: LocalTime?): String? {
        return localTime?.toString()
    }
    
    /** 字符串转为 kotlinx.datetime.LocalTime */
    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it) }
    }
    
    /** kotlinx.datetime.Instant 转为长整型 */
    @TypeConverter
    fun instantToLong(instant: Instant?): Long? {
        return instant?.toEpochMilliseconds()
    }
    
    /** 长整型转为 kotlinx.datetime.Instant */
    @TypeConverter
    fun longToInstant(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }
}
