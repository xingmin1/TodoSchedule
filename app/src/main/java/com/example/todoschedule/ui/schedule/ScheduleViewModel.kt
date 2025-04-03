package com.example.todoschedule.ui.schedule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.model.User
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.domain.repository.UserRepository
import com.example.todoschedule.domain.utils.CalendarUtils
import com.example.todoschedule.ui.schedule.model.ScheduleUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/** 课程表视图模型 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel
@Inject
constructor(
    private val courseRepository: CourseRepository,
    userRepository: UserRepository,
    private val tableRepository: TableRepository,
    private val globalSettingRepository: GlobalSettingRepository,
) : ViewModel() {

    // 当前选择的周次
    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek

    // 当前周的日期列表
    private val _weekDates = MutableStateFlow<List<LocalDate>>(emptyList())
    val weekDates: StateFlow<List<LocalDate>> = _weekDates

    // 当前用户状态
    private val currentUserState: StateFlow<User?> = userRepository.getCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // 当前默认课表ID状态
    private val _defaultTableIdState: StateFlow<Int> = currentUserState.flatMapLatest { user ->
        if (user != null) {
            globalSettingRepository.getDefaultTableIds(user.id)
                .map { it.firstOrNull() ?: AppConstants.Ids.INVALID_TABLE_ID }
        } else {
            flowOf(AppConstants.Ids.INVALID_TABLE_ID)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppConstants.Ids.INVALID_TABLE_ID
    )

    val defaultTableIdState: StateFlow<Int> = _defaultTableIdState

    // 当前课表状态
    private val currentTableState: StateFlow<Table?> =
        _defaultTableIdState.flatMapLatest { tableId ->
            if (tableId != AppConstants.Ids.INVALID_TABLE_ID) {
                tableRepository.getTableById(tableId)
            } else {
                flowOf(null)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // UI状态 - 声明式定义
    val uiState: StateFlow<ScheduleUiState> = combine(
        currentUserState,
        _defaultTableIdState,
        currentTableState
    ) { user, tableId, table ->
        // 添加日志以便调试
        Log.d(
            "ScheduleViewModel",
            "State combine for UI: user=${user?.id}, tableId=$tableId, table=${table?.id}"
        )
        when {
            // 还在等待用户数据加载 (初始状态 currentUserState.value 可能不是 null)
            user == null && tableId == AppConstants.Ids.INVALID_TABLE_ID && table == null -> {
                Log.d("ScheduleViewModel", "UI State: Initial Loading")
                ScheduleUiState.Loading
            }
            // 用户不存在 (currentUserState 不为 null，但 user 为 null)
            user == null && currentUserState.value != null -> {
                Log.w("ScheduleViewModel", "UI State: No user found.")
                ScheduleUiState.Error("未找到用户")
            }
            // 用户存在，没有设置默认课表
            user != null && tableId == AppConstants.Ids.INVALID_TABLE_ID && _defaultTableIdState.value != AppConstants.Ids.INVALID_TABLE_ID -> {
                Log.w("ScheduleViewModel", "UI State: No default table selected.")
                ScheduleUiState.Success // 允许无默认课表状态，UI应提示
            }
            // 用户存在，有默认课表ID，但无法加载有效的课表
            user != null && tableId != AppConstants.Ids.INVALID_TABLE_ID && table == null && currentTableState.value != null -> {
                Log.e("ScheduleViewModel", "UI State: Error loading table ID $tableId")
                ScheduleUiState.Error("加载课表失败")
            }
            // 成功加载所有必要数据 (用户和课表)
            user != null && table != null -> {
                Log.d(
                    "ScheduleViewModel",
                    "UI State: Success - User: ${user.id}, Table: ${table.id}"
                )
                ScheduleUiState.Success
            }
            // 其他情况视为仍在加载中 (例如，用户已加载，正在加载课表)
            else -> {
                Log.d("ScheduleViewModel", "UI State: Fallback to Loading")
                ScheduleUiState.Loading
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScheduleUiState.Loading // 初始状态为 Loading
    )

    // 当前周的课程列表
    val weekCourses: StateFlow<List<Course>> = combine(
        _defaultTableIdState,
        _currentWeek
    ) { tableId, week ->
        tableId to week
    }.flatMapLatest { (tableId, week) ->
        if (tableId <= 0) {
            Log.d("ScheduleViewModel", "weekCourses - Invalid tableId: $tableId")
            flowOf(emptyList())
        } else {
            Log.d(
                "ScheduleViewModel",
                "weekCourses - Fetching courses for tableId: $tableId, week: $week"
            )
            courseRepository.getCoursesByTableId(tableId).map { courses ->
                Log.d(
                    "ScheduleViewModel",
                    "weekCourses - Original courses for table $tableId: ${courses.size}"
                )
                courses.map { course ->
                    course.copy(
                        nodes = course.nodes.filter { node -> node.isInWeek(week) }
                    )
                }.filter { it.nodes.isNotEmpty() }
                    .also {
                        Log.d(
                            "ScheduleViewModel",
                            "weekCourses - Filtered courses for week $week: ${it.size}"
                        )
                    }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        Log.d("ScheduleViewModel", "ViewModel initialized")

        // 观察课表状态以初始化当前周 (这个仍然需要，用于设置初始周)
        viewModelScope.launch {
            currentTableState.collect { table ->
                val startDate = table?.startDate
                val currentWeekNumber = CalendarUtils.getCurrentWeek(startDate)
                if (_currentWeek.value != currentWeekNumber) {
                    Log.d(
                        "ScheduleViewModel",
                        "Initializing current week to $currentWeekNumber based on table start date $startDate"
                    )
                    _currentWeek.value = currentWeekNumber
                }
                updateWeekDates(currentWeekNumber, startDate) // 更新日期列表
            }
        }
    }

    // --- UI 事件处理 ---

    /** 更新当前周 */
    fun updateCurrentWeek(week: Int) {
        if (week > 0 && week != _currentWeek.value) {
            Log.d("ScheduleViewModel", "Updating current week to $week")
            val currentStartDate = currentTableState.value?.startDate
            _currentWeek.value = week
            updateWeekDates(week, currentStartDate)
        }
    }

    /** 更新周日期 (私有辅助函数) */
    private fun updateWeekDates(week: Int, startDate: LocalDate?) {
        val newDates = CalendarUtils.getWeekDates(week, startDate)
        if (_weekDates.value != newDates) {
            Log.d("ScheduleViewModel", "Updating week dates for week $week, startDate $startDate")
            _weekDates.value = newDates
        }
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
        // 考虑课表总周数限制，如果 Table 模型有 totalWeeks 字段会更好
        if (newWeek <= CalendarUtils.MAX_WEEKS) { // 使用常量或从课表获取
            updateCurrentWeek(newWeek)
        }
    }

    /** 回到当前周 */
    fun goToCurrentWeek() {
        val currentStartDate = currentTableState.value?.startDate
        val currentWeekNumber = CalendarUtils.getCurrentWeek(currentStartDate)
        Log.d("ScheduleViewModel", "Going back to current week: $currentWeekNumber")
        updateCurrentWeek(currentWeekNumber)
    }

    /** 获取特定日期的课程节点 (优化为使用 StateFlow 的值) */
    fun getCourseNodesByDay(dayOfWeek: Int): List<CourseNode> {
        // 直接从 weekCourses 的当前值过滤，无需额外 Flow
        return weekCourses.value.flatMap { course ->
            course.nodes.filter { node -> node.day == dayOfWeek }
        }
    }
}
