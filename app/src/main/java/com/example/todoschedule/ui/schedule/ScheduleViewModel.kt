package com.example.todoschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.domain.utils.CalendarUtils
import com.example.todoschedule.ui.schedule.model.ScheduleUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 课程表视图模型
 */
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    // 当前选择的周次
    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek

    // 当前周的日期列表
    private val _weekDates = MutableStateFlow<List<LocalDate>>(emptyList())
    val weekDates: StateFlow<List<LocalDate>> = _weekDates

    // 当前周的课程列表
    val weekCourses = combine(
        _currentWeek,
        courseRepository.getCoursesByTableId(1) //TODO: 取消默认使用第一个课表
    ) { week, courses ->
        courses.map { course ->
            course.copy(
                nodes = course.nodes.filter { node -> node.isInWeek(week) }
            )
        }.filter { it.nodes.isNotEmpty() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 加载状态
    private val _uiState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Loading)
    val uiState: StateFlow<ScheduleUiState> = _uiState

    init {
        loadScheduleData()
    }

    /**
     * 加载课程表数据
     */
    private fun loadScheduleData() {
        viewModelScope.launch {
            try {
                // 计算当前是第几周
                val currentWeekNumber = CalendarUtils.getCurrentWeek()
                _currentWeek.value = currentWeekNumber
                
                // 计算本周日期
                updateWeekDates(currentWeekNumber)
                
                _uiState.value = ScheduleUiState.Success
            } catch (e: Exception) {
                _uiState.value = ScheduleUiState.Error(e.message ?: "未知错误")
            }
        }
    }

    /**
     * 更新当前周
     */
    fun updateCurrentWeek(week: Int) {
        _currentWeek.value = week
        updateWeekDates(week)
    }

    /**
     * 更新周日期
     */
    private fun updateWeekDates(week: Int) {
        _weekDates.value = CalendarUtils.getWeekDates(week)
    }

    /**
     * 上一周
     */
    fun previousWeek() {
        val newWeek = _currentWeek.value - 1
        if (newWeek > 0) {
            updateCurrentWeek(newWeek)
        }
    }

    /**
     * 下一周
     */
    fun nextWeek() {
        val newWeek = _currentWeek.value + 1
        if (newWeek <= CalendarUtils.MAX_WEEKS) {
            updateCurrentWeek(newWeek)
        }
    }

    /**
     * 回到当前周
     */
    fun goToCurrentWeek() {
        val currentWeekNumber = CalendarUtils.getCurrentWeek()
        updateCurrentWeek(currentWeekNumber)
    }

    /**
     * 获取特定日期的课程节点
     */
    fun getCourseNodesByDay(day: Int): List<CourseNode> {
        return weekCourses.value.flatMap { course ->
            course.nodes.filter { it.day == day }
        }
    }

    /**
     * 删除课程
     */
    fun deleteCourse(courseId: Int) {
        viewModelScope.launch {
            try {
                courseRepository.deleteCourse(courseId)
                // 重新加载数据
                loadScheduleData()
            } catch (e: Exception) {
                _uiState.value = ScheduleUiState.Error(e.message ?: "删除课程失败")
            }
        }
    }
}