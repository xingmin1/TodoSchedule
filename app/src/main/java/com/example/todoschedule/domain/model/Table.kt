package com.example.todoschedule.domain.model

/**
 * 课表领域模型
 */
data class Table(
    val id: Int = 0,
    val tableName: String,
    val background: String = "",
    val startDate: String,
    val totalWeeks: Int = 20,
    val showSat: Boolean = true,
    val showSun: Boolean = true
) 