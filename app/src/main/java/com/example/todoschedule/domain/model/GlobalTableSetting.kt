package com.example.todoschedule.domain.model

/** 全局课表设置领域模型 */
data class GlobalTableSetting(
    val Id: UUID = 0,
    val userId: UUID,
    val defaultTableIds: List<Int> = emptyList(),
    val showWeekend: Boolean = true,
    val courseNotificationStyle: Int = 0,
    val notifyBeforeMinutes: Int = 15,
    val autoSwitchWeek: Boolean = true,
    val showCourseTime: Boolean = true
)
