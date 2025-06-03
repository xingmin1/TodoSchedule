package com.example.todoschedule.ui.course.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.domain.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 课程详情ViewModel
 */
@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow<CourseDetailUiState>(CourseDetailUiState.Loading)
    val uiState: StateFlow<CourseDetailUiState> = _uiState

    // 当前课程ID
    private var currentCourseId: UUID = 0

    /**
     * 加载课程详情
     */
    fun loadCourse(courseId: UUID) {
        currentCourseId = courseId
        _uiState.value = CourseDetailUiState.Loading

        viewModelScope.launch {
            try {
                val course = courseRepository.getCourseById(courseId)
                if (course != null) {
                    _uiState.value = CourseDetailUiState.Success(course.toDetailModel())
                } else {
                    _uiState.value = CourseDetailUiState.Error("找不到课程")
                }
            } catch (e: Exception) {
                _uiState.value = CourseDetailUiState.Error(e.message ?: "加载课程失败")
            }
        }
    }

    /**
     * 删除课程
     */
    fun deleteCourse() {
        if (currentCourseId <= 0) return

        viewModelScope.launch {
            try {
                courseRepository.deleteCourse(currentCourseId)
                _uiState.value = CourseDetailUiState.Deleted
            } catch (e: Exception) {
                _uiState.value = CourseDetailUiState.Error(e.message ?: "删除课程失败")
            }
        }
    }

    /**
     * 将Course领域模型转换为CourseDetailModel UI模型
     */
    private fun Course.toDetailModel(): CourseDetailModel {
        return CourseDetailModel(
            id = id,
            courseName = courseName,
            color = color,
            room = room,
            teacher = teacher,
            credit = credit,
            courseCode = courseCode,
            nodes = nodes.map { it.toDetailModel() }
        )
    }

    /**
     * 将CourseNode领域模型转换为CourseNodeDetailModel UI模型
     */
    private fun CourseNode.toDetailModel(): CourseNodeDetailModel {
        return CourseNodeDetailModel(
            day = day,
            startNode = startNode,
            step = step,
            startWeek = startWeek,
            endWeek = endWeek,
            weekType = weekType,
            room = room,
            teacher = teacher
        )
    }
} 