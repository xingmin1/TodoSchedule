package com.example.todoschedule.ui.home

import com.example.todoschedule.data.database.converter.ScheduleStatus
import com.example.todoschedule.ui.theme.ColorSchemeEnum
import kotlinx.datetime.LocalTime

/**
 * 首页课程UI模型
 */
data class HomeCourseUiModel(
    val id: Int,
    val name: String,
    val startNode: Int,
    val endNode: Int,
    val step: Int,
    val location: String? = null,
    val color: ColorSchemeEnum,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val timeDisplay: String
)

/**
 * 首页日程UI模型
 */
data class HomeScheduleUiModel(
    val id: Int,
    val title: String,
    val description: String?,
    val isCompleted: Boolean,
    val status: ScheduleStatus = ScheduleStatus.TODO,
    val startTime: Long,
    val endTime: Long,
    val priority: Int? = null
)

/**
 * 首页学习统计UI模型
 */
data class HomeStudyStatUiModel(
    val weeklyFocusTime: Float,
    val progress: Float,
    val changePercentage: Float
)