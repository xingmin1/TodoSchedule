package com.example.todoschedule.domain.model

import com.example.todoschedule.core.constants.AppConstants.EMPTY_UUID
import java.util.UUID

/** 全局课表设置领域模型 */
data class GlobalTableSetting(
    val id: UUID,
    val userId: UUID,
    val defaultTableIds: List<Int> = emptyList(),
    val showWeekend: Boolean = true,
    val courseNotificationStyle: Int = 0,
    val notifyBeforeMinutes: Int = 15,
    val autoSwitchWeek: Boolean = true,
    val showCourseTime: Boolean = true
)
