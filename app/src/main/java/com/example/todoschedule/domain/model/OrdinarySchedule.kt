package com.example.todoschedule.domain.model

import com.example.todoschedule.core.constants.AppConstants.EMPTY_UUID
import com.example.todoschedule.data.database.converter.ScheduleStatus // 引入枚举
import com.example.todoschedule.ui.theme.ColorSchemeEnum
import java.util.UUID

/**
 * Domain 层的 OrdinarySchedule 模型，包含其关联的 TimeSlot 列表。
 */
data class OrdinarySchedule(
    val id: UUID = EMPTY_UUID,
    val userId: UUID,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val category: String? = null,
    val color: ColorSchemeEnum? = null,
    val isAllDay: Boolean = false,
    val status: ScheduleStatus? = ScheduleStatus.TODO, // 使用枚举类型
    val timeSlots: List<TimeSlot> = emptyList() // 关联的时间槽
) 