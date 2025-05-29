package com.example.todoschedule.data.database.converter

import androidx.room.TypeConverter
import com.tap.hlc.Timestamp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json

/** Room类型转换器 */
class Converters {
    private val json = Json { ignoreUnknownKeys = true }
    
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

    /** com.tap.hlc.Timestamp 转为长整型，使用明确的类型声明避免类型推断问题 */
    @TypeConverter
    fun hlcTimestampToLong(timestamp: com.tap.hlc.Timestamp?): Long? {
        return timestamp?.epochMillis
    }

    /** 长整型转为 com.tap.hlc.Timestamp，使用明确的类型声明避免类型推断问题 */
    @TypeConverter
    fun longToHlcTimestamp(value: Long?): com.tap.hlc.Timestamp? {
        return value?.let { com.tap.hlc.Timestamp(it) }
    }

    // --- ScheduleStatus Converters ---
    @TypeConverter
    fun fromScheduleStatus(status: ScheduleStatus?): String? {
        return status?.name // 存储为枚举名称字符串
    }

    @TypeConverter
    fun toScheduleStatus(statusString: String?): ScheduleStatus? {
        return statusString?.let { enumValueOf<ScheduleStatus>(it) } // 从字符串恢复枚举
    }

    // --- ScheduleType Converters ---
    @TypeConverter
    fun fromScheduleType(type: ScheduleType): String {
        return type.name
    }

    @TypeConverter
    fun toScheduleType(typeString: String): ScheduleType {
        // 如果数据库可能包含无效值，这里可以添加错误处理
        return enumValueOf<ScheduleType>(typeString)
    }

    // --- ReminderType Converters ---
    @TypeConverter
    fun fromReminderType(type: ReminderType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toReminderType(typeString: String?): ReminderType? {
        return typeString?.let { enumValueOf<ReminderType>(it) }
    }
}
