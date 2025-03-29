package com.example.todoschedule.ui.course

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 课程详情视图模型
 */
@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<CourseDetailUiState>(CourseDetailUiState.Loading)
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()
    
    // 从参数中获取课程ID
    private val courseId: Int = checkNotNull(savedStateHandle["courseId"])
    
    init {
        loadCourse(courseId)
    }
    
    /**
     * 加载课程信息
     */
    fun loadCourse(id: Int) {
        viewModelScope.launch {
            _uiState.value = CourseDetailUiState.Loading
            try {
                val course = courseRepository.getCourseById(id)
                if (course != null) {
                    TODO()
                    // _uiState.value = CourseDetailUiState.Success(
                    //     CourseDetailModel(
                    //         id = course.id,
                    //         name = course.name,
                    //         teacher = course.teacher ?: "",
                    //         location = course.location ?: "",
                    //         dayOfWeek = course.dayOfWeek,
                    //         startNode = course.startNode,
                    //         endNode = course.endNode,
                    //         weeks = course.courseNodes.map { it.week }.sorted()
                    //     )
                    // )
                } else {
                    _uiState.value = CourseDetailUiState.Error("未找到该课程")
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
        viewModelScope.launch {
            try {
                courseRepository.deleteCourse(courseId)
            } catch (e: Exception) {
                // 错误处理可以通过UI状态或事件通知用户
                e.printStackTrace()
            }
        }
    }
}

/**
 * 课程详情UI状态
 */
sealed class CourseDetailUiState {
    object Loading : CourseDetailUiState()
    data class Error(val message: String) : CourseDetailUiState()
    data class Success(val course: CourseDetailModel) : CourseDetailUiState()
}

/**
 * 课程详情UI模型
 */
data class CourseDetailModel(
    val id: Int,
    val name: String,
    val teacher: String,
    val location: String,
    val dayOfWeek: Int,
    val startNode: Int,
    val endNode: Int,
    val weeks: List<Int>
) 