package com.example.todoschedule.data.remote.model

/**
 * 课表数据传输对象
 */
data class TableDto(
    val id: Int = 0,
    val userId: Int = 0,
    val tableName: String,
    val background: String = "",
    val listPosition: Int = 0,
    val terms: String = "",
    val startDate: String,
    val totalWeeks: Int = 20
) 