package com.example.todoschedule.ui.schedule.model

import java.util.UUID

/**
 * 课程UI显示模型
 */
data class CourseUiModel(
    val id: UUID,
    val name: String,
    val room: String?,
    val teacher: String?,
    val color: String,
    val startNode: Int,
    val step: Int,
    val day: Int,
    val isCurrentWeek: Boolean
) 