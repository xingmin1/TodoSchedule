package com.example.todoschedule.domain.model

import com.example.todoschedule.data.database.converter.ScheduleStatus // 引入枚举

/**
 * Domain 层的 OrdinarySchedule 模型，包含其关联的 TimeSlot 列表。
 */
data class OrdinarySchedule(
    val id: Int = 0,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val category: String? = null,
    val color: String? = null,
    val isAllDay: Boolean = false,
    val status: ScheduleStatus? = ScheduleStatus.TODO, // 使用枚举类型
    val timeSlots: List<TimeSlot> = emptyList() // 关联的时间槽
) 