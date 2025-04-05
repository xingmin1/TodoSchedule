package com.example.todoschedule.domain.model

import kotlinx.datetime.LocalTime

/**
 * 课表时间配置节点的 Domain 模型。
 */
data class TableTimeConfigNode(
    val id: Int,
    val name: String, // 节次名称
    val startTime: LocalTime, // 开始时间
    val endTime: LocalTime, // 结束时间
    val node: Int // 第几节
) 