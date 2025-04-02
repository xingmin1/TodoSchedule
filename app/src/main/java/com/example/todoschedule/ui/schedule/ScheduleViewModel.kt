package com.example.todoschedule.ui.schedule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.DatabaseInitializer
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.domain.repository.UserRepository
import com.example.todoschedule.domain.utils.CalendarUtils
import com.example.todoschedule.ui.schedule.model.ScheduleUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/** 课程表视图模型 */
@HiltViewModel
class ScheduleViewModel
@Inject
constructor(
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    private val tableRepository: TableRepository,
    private val globalSettingRepository: GlobalSettingRepository,
    private val databaseInitializer: DatabaseInitializer
) : ViewModel() {

    // 当前选择的周次
    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek

    // 当前周的日期列表
    private val _weekDates = MutableStateFlow<List<LocalDate>>(emptyList())
    val weekDates: StateFlow<List<LocalDate>> = _weekDates

    // 加载状态
    private val _uiState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Loading)
    val uiState: StateFlow<ScheduleUiState> = _uiState

    // 初始化状态
    private val _dataLoaded = MutableStateFlow(false)

    // 当前用户ID
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentUserId = MutableStateFlow(AppConstants.Ids.INVALID_USER_ID)

    // 当前默认课表ID
    private val _defaultTableId = MutableStateFlow(AppConstants.Ids.INVALID_TABLE_ID)
    val defaultTableId: StateFlow<Int> = _defaultTableId

    // 当前课表的开始日期
    private val _tableStartDate = MutableStateFlow<LocalDate?>(null)
    val tableStartDate: StateFlow<LocalDate?> = _tableStartDate

    // 当前周的课程列表
    @OptIn(ExperimentalCoroutinesApi::class)
    val weekCourses: StateFlow<List<Course>> = combine(
        _defaultTableId,
        _dataLoaded
    ) { tableId, isDataLoaded ->
        // 创建一个 Pair，包含 tableId 和加载状态，方便后续处理
        tableId to isDataLoaded
    }.flatMapLatest { (tableId, isDataLoaded) ->
        // 检查 tableId 是否有效以及数据是否已加载
        if (tableId <= 0 || !isDataLoaded) {
            Log.d("ScheduleViewModel", "flatMapLatest - 无效 tableId 或数据未加载")
            // 返回一个持续发出空列表的 Flow
            flowOf(emptyList<Course>())
        } else {
            Log.d("ScheduleViewModel", "flatMapLatest - 有效 tableId: $tableId，数据已加载")
            // tableId 有效且数据已加载，现在结合 currentWeek 和课程数据流
            combine(
                _currentWeek,
                courseRepository.getCoursesByTableId(tableId) // 直接观察课程数据流
            ) { week, courses ->
                Log.d(
                    "ScheduleViewModel",
                    "Inner combine - week: $week, 原始课程数: ${courses.size}"
                )
                // 在这里进行按周过滤
                courses
                    .map {
                        it.copy(
                            nodes = it.nodes.filter { node -> node.isInWeek(week) }
                        )
                    }
                    .filter { it.nodes.isNotEmpty() }
                    .also { Log.d("ScheduleViewModel", "Inner combine - 过滤后课程数: ${it.size}") }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // 等待数据库初始化完成后再加载数据
        viewModelScope.launch {
            databaseInitializer.isInitialized.collect { initialized ->
                if (initialized) {
                    // 数据库已初始化，可以安全加载数据
                    loadUserAndSettings()
                    this.cancel()
                }
            }
        }
    }

    /** 加载用户和设置数据 */
    private fun loadUserAndSettings() {
        viewModelScope.launch {
            try {
                Log.d("ScheduleViewModel", "开始加载用户和设置数据")

                // 获取当前用户
                val user = userRepository.getCurrentUser().first()
                if (user != null) {
                    currentUserId.value = user.id
                    Log.d("ScheduleViewModel", "已获取用户ID: ${user.id}")

                    // 获取默认课表ID
                    val tableIds = globalSettingRepository.getDefaultTableIds(user.id).first()
                    val tableId = tableIds.firstOrNull() ?: AppConstants.Ids.INVALID_TABLE_ID

                    if (tableId > 0) {
                        _defaultTableId.value = tableId
                        Log.d("ScheduleViewModel", "已获取默认课表ID: $tableId")

                        // 获取课表信息
                        val table = tableRepository.getTableById(tableId)
                        if (table != null) {
                            _tableStartDate.value = table.startDate
                            Log.d("ScheduleViewModel", "获取到课表开始日期: ${table.startDate}")
                        } else {
                            Log.w("ScheduleViewModel", "未找到课表 ID: $tableId")
                        }
                    } else {
                        Log.w("ScheduleViewModel", "用户 ${user.id} 没有默认课表")
                    }
                } else {
                    Log.w("ScheduleViewModel", "未找到有效用户")
                }

                // 计算当前是第几周（使用课表的开始日期）
                val currentWeekNumber = CalendarUtils.getCurrentWeek(_tableStartDate.value)
                _currentWeek.value = currentWeekNumber

                // 计算本周日期
                updateWeekDates(currentWeekNumber)

                // 标记数据已加载
                _dataLoaded.value = true
                _uiState.value = ScheduleUiState.Success

                Log.d("ScheduleViewModel", "用户和设置数据加载完成")
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "加载数据失败: ${e.message}", e)
                _uiState.value = ScheduleUiState.Error(e.message ?: "未知错误")
            }
        }
    }

    /** 更新当前周 */
    fun updateCurrentWeek(week: Int) {
        _currentWeek.value = week
        updateWeekDates(week)
    }

    /** 更新周日期 */
    private fun updateWeekDates(week: Int) {
        _weekDates.value = CalendarUtils.getWeekDates(week, _tableStartDate.value)
    }

    /** 上一周 */
    fun previousWeek() {
        val newWeek = _currentWeek.value - 1
        if (newWeek > 0) {
            updateCurrentWeek(newWeek)
        }
    }

    /** 下一周 */
    fun nextWeek() {
        val newWeek = _currentWeek.value + 1
        if (newWeek <= CalendarUtils.MAX_WEEKS) {
            updateCurrentWeek(newWeek)
        }
    }

    /** 回到当前周 */
    fun goToCurrentWeek() {
        val currentWeekNumber = CalendarUtils.getCurrentWeek(_tableStartDate.value)
        updateCurrentWeek(currentWeekNumber)
    }

    /** 获取特定日期的课程节点 */
    fun getCourseNodesByDay(day: Int): List<CourseNode> {
        return weekCourses.value.flatMap { course ->
            course.nodes.filter { node -> node.day == day }
        }
    }

    /** 删除课程 */
    fun deleteCourse(courseId: Int) {
        viewModelScope.launch {
            try {
                courseRepository.deleteCourse(courseId)
                // 无需重新加载所有数据，只需要更新UI状态
                _uiState.value = ScheduleUiState.Success
            } catch (e: Exception) {
                _uiState.value = ScheduleUiState.Error(e.message ?: "删除课程失败")
            }
        }
    }
}
