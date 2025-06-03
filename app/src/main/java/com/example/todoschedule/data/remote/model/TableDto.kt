package com.example.todoschedule.data.remote.model

import java.util.UUID

/**
 * 课表数据传输对象
 */
data class TableDto(
    val id: UUID,
    val userId: UUID,
    val tableName: String,
    val background: String = "",
    val listPosition: Int = 0,
    val terms: String = "",
    val startDate: String,
    val totalWeeks: Int = 20
) 