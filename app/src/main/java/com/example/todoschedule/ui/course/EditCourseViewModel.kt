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
 * 编辑课程视图模型
 */
@HiltViewModel
class EditCourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<EditCourseUiState>(EditCourseUiState.Loading)
    val uiState: StateFlow<EditCourseUiState> = _uiState.asStateFlow()
    
    // 从参数中获取课程ID
    private val courseId: Int = checkNotNull(savedStateHandle["courseId"])
    
    init {
        loadCourseData(courseId)
    }
    
    /**
     * 加载课程数据
     */
    fun loadCourseData(id: Int) {
        viewModelScope.launch {
            _uiState.value = EditCourseUiState.Loading
            try {
                val course = courseRepository.getCourseById(id)
                if (course != null) {
                    TODO()
                    // _uiState.value = EditCourseUiState.Success(
                    //     courseName = course.name,
                    //     teacherName = course.teacher ?: "",
                    //     location = course.location ?: "",
                    //     selectedWeeks = course.courseNodes.map { it.week }.sorted(),
                    //     dayOfWeek = course.dayOfWeek,
                    //     startPeriod = course.startNode,
                    //     endPeriod = course.endNode
                    // )
                } else {
                    _uiState.value = EditCourseUiState.Error("未找到该课程")
                }
            } catch (e: Exception) {
                _uiState.value = EditCourseUiState.Error(e.message ?: "加载课程失败")
            }
        }
    }
    
    /**
     * 更新课程名称
     */
    fun updateCourseName(name: String) {
        val currentState = _uiState.value
        if (currentState is EditCourseUiState.Success) {
            _uiState.value = currentState.copy(courseName = name)
        }
    }
    
    /**
     * 更新教师姓名
     */
    fun updateTeacherName(name: String) {
        val currentState = _uiState.value
        if (currentState is EditCourseUiState.Success) {
            _uiState.value = currentState.copy(teacherName = name)
        }
    }
    
    /**
     * 更新课程地点
     */
    fun updateLocation(location: String) {
        val currentState = _uiState.value
        if (currentState is EditCourseUiState.Success) {
            _uiState.value = currentState.copy(location = location)
        }
    }
    
    /**
     * 更新单个周次的选择状态
     */
    fun updateSelectedWeek(week: Int, selected: Boolean) {
        val currentState = _uiState.value
        if (currentState is EditCourseUiState.Success) {
            val currentWeeks = currentState.selectedWeeks.toMutableList()
            if (selected && !currentWeeks.contains(week)) {
                currentWeeks.add(week)
            } else if (!selected) {
                currentWeeks.remove(week)
            }
            _uiState.value = currentState.copy(selectedWeeks = currentWeeks.sorted())
        }
    }
    
    /**
     * 更新星期
     */
    fun updateDayOfWeek(day: Int) {
        val currentState = _uiState.value
        if (currentState is EditCourseUiState.Success) {
            _uiState.value = currentState.copy(dayOfWeek = day)
        }
    }
    
    /**
     * 更新开始节数
     */
    fun updateStartPeriod(period: Int) {
        val currentState = _uiState.value
        if (currentState is EditCourseUiState.Success) {
            val currentEndPeriod = currentState.endPeriod
            _uiState.value = currentState.copy(
                startPeriod = period,
                // 如果开始节数大于结束节数，则更新结束节数为开始节数
                endPeriod = if (period > currentEndPeriod) period else currentEndPeriod
            )
        }
    }
    
    /**
     * 更新结束节数
     */
    fun updateEndPeriod(period: Int) {
        val currentState = _uiState.value
        if (currentState is EditCourseUiState.Success) {
            _uiState.value = currentState.copy(endPeriod = period)
        }
    }
    
    /**
     * 更新课程信息
     */
    fun updateCourse() {
        val currentState = _uiState.value
        if (currentState is EditCourseUiState.Success) {
            if (currentState.courseName.isBlank() || currentState.selectedWeeks.isEmpty()) {
                return
            }
            
            viewModelScope.launch {
                try {
                    // 更新课程信息
                    TODO()
                    // courseRepository.updateCourse(
                    //     courseId = courseId,
                    //     name = currentState.courseName,
                    //     teacher = currentState.teacherName,
                    //     location = currentState.location,
                    //     dayOfWeek = currentState.dayOfWeek,
                    //     startNode = currentState.startPeriod,
                    //     endNode = currentState.endPeriod,
                    //     weeks = currentState.selectedWeeks
                    // )
                } catch (e: Exception) {
                    // 处理错误
                    e.printStackTrace()
                }
            }
        }
    }
}

/**
 * 编辑课程UI状态
 */
sealed class EditCourseUiState {
    object Loading : EditCourseUiState()
    data class Error(val message: String) : EditCourseUiState()
    data class Success(
        val courseName: String,
        val teacherName: String,
        val location: String,
        val selectedWeeks: List<Int>,
        val dayOfWeek: Int,
        val startPeriod: Int,
        val endPeriod: Int
    ) : EditCourseUiState()
} 