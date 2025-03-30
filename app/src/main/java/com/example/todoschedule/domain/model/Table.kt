package com.example.todoschedule.domain.model

import kotlinx.datetime.LocalDate

/** 课表领域模型 */
data class Table(
    val id: Int = 0,
    val userId: Int = 1,
    val tableName: String,
    val background: String = "",
    val startDate: LocalDate,
    val totalWeeks: Int = 20,
    val showSat: Boolean = true,
    val showSun: Boolean = true
)
