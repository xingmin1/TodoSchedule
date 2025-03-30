package com.example.todoschedule.domain.model

import kotlinx.datetime.LocalTime

/**
 * 时间详情领域模型
 */
data class TimeDetail(
    val node: Int,
    val name: String,
    val startTime: LocalTime,
    val endTime: LocalTime
) 