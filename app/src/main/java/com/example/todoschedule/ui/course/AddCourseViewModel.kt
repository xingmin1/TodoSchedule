package com.example.todoschedule.ui.course

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.todoschedule.domain.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 添加课程视图模型
 */
@HiltViewModel
class AddCourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddCourseUiState())
    val uiState: StateFlow<AddCourseUiState> = _uiState.asStateFlow()
    
    // 从参数中获取课程表ID
    private val tableId: Int = checkNotNull(savedStateHandle["tableId"])
    
    init {
        // 初始化默认选择周次为1-16周
        updateSelectedWeeks((1..16).toList())
    }
    
    /**
     * 更新课程名称
     */
    fun updateCourseName(name: String) {
        _uiState.value = _uiState.value.copy(courseName = name)
    }
    
    /**
     * 更新教师姓名
     */
    fun updateTeacherName(name: String) {
        _uiState.value = _uiState.value.copy(teacherName = name)
    }
    
    /**
     * 更新课程地点
     */
    fun updateLocation(location: String) {
        _uiState.value = _uiState.value.copy(location = location)
    }
    
    /**
     * 更新选择的周次
     */
    private fun updateSelectedWeeks(weeks: List<Int>) {
        _uiState.value = _uiState.value.copy(selectedWeeks = weeks)
    }
    
    /**
     * 更新单个周次的选择状态
     */
    fun updateSelectedWeek(week: Int, selected: Boolean) {
        val currentWeeks = _uiState.value.selectedWeeks.toMutableList()
        if (selected && !currentWeeks.contains(week)) {
            currentWeeks.add(week)
        } else if (!selected) {
            currentWeeks.remove(week)
        }
        updateSelectedWeeks(currentWeeks.sorted())
    }
    
    /**
     * 更新星期
     */
    fun updateDayOfWeek(day: Int) {
        _uiState.value = _uiState.value.copy(dayOfWeek = day)
    }
    
    /**
     * 更新开始节数
     */
    fun updateStartPeriod(period: Int) {
        val currentEndPeriod = _uiState.value.endPeriod
        _uiState.value = _uiState.value.copy(
            startPeriod = period,
            // 如果开始节数大于结束节数，则更新结束节数为开始节数
            endPeriod = if (period > currentEndPeriod) period else currentEndPeriod
        )
    }
    
    /**
     * 更新结束节数
     */
    fun updateEndPeriod(period: Int) {
        _uiState.value = _uiState.value.copy(endPeriod = period)
    }
    
    /**
     * 保存课程信息
     */
    fun saveCourse() {
        // TODO:
        // val state = _uiState.value
        // if (state.courseName.isBlank() || state.selectedWeeks.isEmpty()) {
        //     return
        // }
        //
        // viewModelScope.launch {
        //     try {
        //         // 从UI状态创建课程实体
        //         // 这里只是一个示例，具体实现取决于CourseRepository的API
        //         courseRepository.addCourse(
        //             Course(
        //                 name = state.courseName,
        //                 teacher = state.teacherName,
        //                 location = state.location,
        //                 dayOfWeek = state.dayOfWeek,
        //                 startNode = state.startPeriod,
        //                 endNode = state.endPeriod,
        //                 weeks = state.selectedWeeks
        //             )
        //         )
        //     } catch (e: Exception) {
        //         // 处理错误
        //         e.printStackTrace()
        //     }
        // }
    }
}

/**
 * 添加课程UI状态
 */
data class AddCourseUiState(
    val courseName: String = "",
    val teacherName: String = "",
    val location: String = "",
    val selectedWeeks: List<Int> = emptyList(),
    val dayOfWeek: Int = 1, // 1-7 代表周一到周日
    val startPeriod: Int = 1,
    val endPeriod: Int = 2
) 