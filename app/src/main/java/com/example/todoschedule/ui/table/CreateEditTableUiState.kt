package com.example.todoschedule.ui.table

import kotlinx.datetime.LocalDate

data class CreateEditTableUiState(
    val tableId: UUID? = null, // 用于区分是创建还是编辑
    val tableName: String = "",
    val startDate: LocalDate? = null,
    val totalWeeks: String = "20", // 默认总周数
    val background: String? = null, // 可选背景
    val terms: String? = null, // 可选学期信息

    val showStartDatePicker: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false // 标记保存成功
) {
    val isEditMode: Boolean get() = tableId != null
} 