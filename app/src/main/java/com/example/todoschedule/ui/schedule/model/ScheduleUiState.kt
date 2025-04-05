package com.example.todoschedule.ui.schedule.model

/**
 * 课表界面UI状态
 */
sealed class ScheduleUiState {
    /**
     * 加载中
     */
    object Loading : ScheduleUiState()

    /**
     * 空状态
     */
    object Empty : ScheduleUiState()

    /**
     * 错误状态
     */
    data class Error(val message: String) : ScheduleUiState()

    /**
     * 用户已登录，但没有默认课表被选中或存在。
     */
    object NoTableSelected : ScheduleUiState()

    /**
     * 成功状态
     */
    object Success : ScheduleUiState()
    // data class Success(
    //     val table: Table,
    //     val courses: List<CourseUiModel>,
    //     val currentWeek: Int,
    //     val selectedWeek: Int,
    //     val timeDetails: List<TimeDetailUiModel>
    // ) : ScheduleUiState()
} 