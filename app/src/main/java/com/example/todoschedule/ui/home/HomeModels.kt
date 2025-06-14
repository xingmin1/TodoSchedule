package com.example.todoschedule.ui.home

import com.example.todoschedule.data.database.converter.ScheduleStatus
import java.util.UUID

/**
 * 首页课程UI模型
 */
data class HomeCourseUiModel(
    val id: UUID,
    val name: String,
    val startNode: Int,
    val endNode: Int,
    val location: String? = null,
    val timeDisplay: String
)

/**
 * 首页日程UI模型
 */
data class HomeScheduleUiModel(
    val id: UUID,
    val title: String,
    val description: String?,
    val status: ScheduleStatus? = ScheduleStatus.TODO,
    val timeDisplay: String,
    val priority: Int? = null
) {
    val isCompleted: Boolean
        get() = status == ScheduleStatus.DONE
}