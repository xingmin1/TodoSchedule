package com.example.todoschedule.domain.model

import com.example.todoschedule.data.database.converter.ReminderType // 引入枚举
import com.example.todoschedule.data.database.converter.ScheduleType // 引入枚举
import kotlinx.datetime.toLocalDateTime

/**
 * Domain 层的 TimeSlot 模型。
 */
data class TimeSlot(
    val id: Int = 0,
    val startTime: Long, // 开始时间 (时间戳)
    val endTime: Long, // 结束时间 (时间戳)
    val scheduleType: ScheduleType, // 使用枚举类型
    val scheduleId: Int, // 关联日程的 ID
    val userId: Int, // 添加 userId
    val head: String? = null, // 可选的标题覆盖
    val priority: Int? = null, // 优先级
    val isCompleted: Boolean = false, // 是否已完成
    val isRepeated: Boolean = false, // 是否重复
    val repeatPattern: String? = null, // 重复模式
    val reminderType: ReminderType? = ReminderType.NONE, // 使用枚举类型
    val reminderOffset: Long? = null, // 提醒偏移量 (毫秒)

    // --- 新增用于 UI 显示的字段 ---
    val displayTitle: String = "", // 要显示的标题 (课程名/日程标题)
    val displaySubtitle: String? = null, // 要显示的副标题 (教室/地点)
    val displayColor: String? = null // 用于背景的颜色字符串 (例如 "#FF0000")


) {
    override fun toString(): String {
        val startTimeFormatted = kotlinx.datetime.Instant.fromEpochMilliseconds(startTime)
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val endTimeFormatted = kotlinx.datetime.Instant.fromEpochMilliseconds(endTime)
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())

        return "TimeSlot(id=$id, startTime=$startTimeFormatted, endTime=$endTimeFormatted, " +
                "scheduleType=${scheduleType}, scheduleId=${scheduleId}, userId=${userId}, head=${head}, " +
                "priority=${priority}, isCompleted=${isCompleted}, isRepeated=${isRepeated}, " +
                "repeatPattern=${repeatPattern}, reminderType=${reminderType}, reminderOffset=${reminderOffset}, " +
                "displayTitle='${displayTitle}', displaySubtitle=${displaySubtitle}, displayColor=${displayColor})"
    }

} 