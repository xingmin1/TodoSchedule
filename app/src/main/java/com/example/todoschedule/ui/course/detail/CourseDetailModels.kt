package com.example.todoschedule.ui.course.detail

import com.example.todoschedule.ui.theme.ColorSchemeEnum

/**
 * 课程详情数据模型
 */
data class CourseDetailModel(
    val Id: UUID,
    val courseName: String,
    val color: ColorSchemeEnum,
    val room: String? = null,
    val teacher: String? = null,
    val credit: Float? = null,
    val courseCode: String? = null,
    val nodes: List<CourseNodeDetailModel> = emptyList()
)

/**
 * 课程节点详情数据模型
 */
data class CourseNodeDetailModel(
    val day: Int,
    val startNode: Int,
    val step: Int,
    val startWeek: Int,
    val endWeek: Int,
    val weekType: Int,
    val room: String? = null,
    val teacher: String? = null
)

/**
 * 课程详情UI状态
 */
sealed class CourseDetailUiState {
    object Loading : CourseDetailUiState()
    data class Success(val course: CourseDetailModel) : CourseDetailUiState()
    data class Error(val message: String) : CourseDetailUiState()
    object Deleted : CourseDetailUiState()
} 