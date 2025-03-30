package com.example.todoschedule.ui.schedule.model

import kotlinx.datetime.LocalTime

/**
 * 时间详情UI模型
 */
data class TimeDetailUiModel(
    val node: Int,
    val name: String,
    val startTime: LocalTime,
    val endTime: LocalTime
) 