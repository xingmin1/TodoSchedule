package com.example.todoschedule.ui.schedule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.model.TableTimeConfig
import com.example.todoschedule.domain.model.TimeSlot
import com.example.todoschedule.domain.model.User
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.domain.repository.TableTimeConfigRepository
import com.example.todoschedule.domain.repository.UserRepository
import com.example.todoschedule.domain.use_case.ordinary_schedule.AddOrdinaryScheduleUseCase
import com.example.todoschedule.domain.use_case.ordinary_schedule.DeleteOrdinaryScheduleUseCase
import com.example.todoschedule.domain.use_case.ordinary_schedule.GetOrdinarySchedulesUseCase
import com.example.todoschedule.domain.use_case.ordinary_schedule.UpdateOrdinaryScheduleUseCase
import com.example.todoschedule.domain.use_case.table_time_config.GetDefaultTableTimeConfigUseCase
import com.example.todoschedule.domain.utils.CalendarUtils
import com.example.todoschedule.ui.schedule.model.ScheduleUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
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
    // 注入普通日程 Use Cases
    getOrdinarySchedulesUseCase: GetOrdinarySchedulesUseCase,
    private val addOrdinaryScheduleUseCase: AddOrdinaryScheduleUseCase,
    private val updateOrdinaryScheduleUseCase: UpdateOrdinaryScheduleUseCase,
    private val deleteOrdinaryScheduleUseCase: DeleteOrdinaryScheduleUseCase,
    // 注入获取时间配置的 Use Case
    private val getDefaultTableTimeConfigUseCase: GetDefaultTableTimeConfigUseCase,
    // 注入 Repository 以便调用 ensureDefaultTimeConfig
    private val tableTimeConfigRepository: TableTimeConfigRepository
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

    // 获取当前课表的时间配置
    private val currentTableTimeConfig: StateFlow<TableTimeConfig?> =
        _defaultTableIdState.flatMapLatest { tableId ->
            if (tableId != AppConstants.Ids.INVALID_TABLE_ID) {
                Log.d("ScheduleViewModel", "Fetching time config for tableId: $tableId")
                getDefaultTableTimeConfigUseCase(tableId)
                    .map { config ->
                        Log.d(
                            "ScheduleViewModel",
                            "Received time config for table $tableId: ${config != null}"
                        )
                        config
                    }
            } else {
                Log.d(
                    "ScheduleViewModel",
                    "Invalid tableId ($tableId), setting time config to null"
                )
                flowOf(null)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // 获取所有普通日程
    private val ordinarySchedules: StateFlow<List<OrdinarySchedule>> = getOrdinarySchedulesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI状态 - 声明式定义
    val uiState: StateFlow<ScheduleUiState> = combine(
        currentUserState,
        _defaultTableIdState,
        currentTableState,
        currentTableTimeConfig,
        ordinarySchedules
    ) { user, tableId, table, tableTimeConfig, _ ->
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
            // 成功加载所有必要数据 (用户、课表、普通日程)
            user != null && table != null -> {
                Log.d(
                    "ScheduleViewModel",
                    "UI State: Success - User: ${user.id}, Table: ${table.id}, Ordinary Schedules loaded."
                )
                ScheduleUiState.Success
            }
            // 其他情况视为仍在加载中
            else -> {
                Log.d(
                    "ScheduleViewModel",
                    "UI State: Fallback to Loading - Waiting for user, table, or schedules."
                )
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

    // 合并后的、用于UI显示的时间项列表
    val displayableTimeSlots: StateFlow<List<TimeSlot>> = combine(
        ordinarySchedules,
        weekCourses,
        currentTableState.filterNotNull(),
        currentTableTimeConfig.filterNotNull(),
        _currentWeek
    ) { schedules, courses, table, tableTimeConfig, week ->
        Log.d(
            "ScheduleViewModel",
            "Combining displayableTimeSlots: week=$week, tableId=${table.id}, coursesCount=${courses.size}, schedulesCount=${schedules.size}"
        )

        // 1. 过滤普通日程 TimeSlot 到当前周
        val tableStartDate = table.startDate

        // **修正**: 正确计算目标周的周一开始日期
        val firstDayOfWeekTableStarts = tableStartDate.dayOfWeek.isoDayNumber
        val firstMondayOfTable =
            tableStartDate.minus(DatePeriod(days = firstDayOfWeekTableStarts - 1))
        val weekStartLocalDate = firstMondayOfTable.plus(DatePeriod(days = (week - 1) * 7))

        val nextWeekStartLocalDate = weekStartLocalDate.plus(DatePeriod(days = 7))
        val weekStartInstant =
            weekStartLocalDate.atTime(0, 0).toInstant(TimeZone.currentSystemDefault())
        val weekEndInstantExclusive =
            nextWeekStartLocalDate.atTime(0, 0).toInstant(TimeZone.currentSystemDefault())
        val weekStartMillis = weekStartInstant.toEpochMilliseconds()
        val weekEndMillisExclusive = weekEndInstantExclusive.toEpochMilliseconds()

        val ordinaryTimeSlotsInWeek = schedules.flatMap { schedule ->
            schedule.timeSlots.filter { slot ->
                (slot.startTime < weekEndMillisExclusive && slot.endTime > weekStartMillis)
            }.map { slot ->
                slot.copy(
                    displayTitle = slot.head ?: schedule.title,
                    displaySubtitle = schedule.location,
                    displayColor = schedule.color
                )
            }
        }
        Log.d(
            "ScheduleViewModel",
            "Ordinary time slots in week $week: ${ordinaryTimeSlotsInWeek.size}"
        )

        // 2. 将 CourseNode 转换为 TimeSlot (并填充显示字段)
        val courseTimeSlotsInWeek =
            convertCourseNodesToTimeSlots(courses, week, tableTimeConfig, table)
        Log.d("ScheduleViewModel", "Course time slots in week $week: ${courseTimeSlotsInWeek.size}")

        // 3. 合并列表并排序 (可选)
        (ordinaryTimeSlotsInWeek + courseTimeSlotsInWeek).sortedBy { it.startTime }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        Log.d("ScheduleViewModel", "ViewModel initialized")

        // 观察课表状态以初始化当前周
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

        // **新增**: 检查并确保默认时间配置存在
        ensureDefaultTimeConfigExists()
    }

    // **新增**: 检查并确保默认时间配置存在的函数
    private fun ensureDefaultTimeConfigExists() {
        combine(currentTableState, currentTableTimeConfig) { table, config ->
            // 只处理 table 加载完成但 config 尚未加载（或加载为 null）的情况
            table to config
        }
            .distinctUntilChanged() // 避免不必要的重复检查
            .onEach { (table, config) ->
                if (table != null && config == null && table.id != AppConstants.Ids.INVALID_TABLE_ID) {
                    Log.w(
                        "ScheduleViewModel",
                        "Default time config not found for table ${table.id}. Attempting to create one."
                    )
                    try {
                        // 在后台协程中调用 ensureDefaultTimeConfig
                        // 注意：这里只是触发创建，不会立即影响 currentTableTimeConfig 的当前值
                        // currentTableTimeConfig 流会在数据插入后自动更新（如果 DAO 返回 Flow）
                        tableTimeConfigRepository.ensureDefaultTimeConfig(table.id)
                        Log.i(
                            "ScheduleViewModel",
                            "ensureDefaultTimeConfig called for table ${table.id}."
                        )
                    } catch (e: Exception) {
                        Log.e(
                            "ScheduleViewModel",
                            "Error ensuring default time config for table ${table.id}",
                            e
                        )
                    }
                }
            }
            .launchIn(viewModelScope) // 在 ViewModel 的作用域内启动 Flow 收集
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

    /** 获取特定日期的课程节点 (这个方法可能不再直接被 UI 使用，UI 应使用 displayableTimeSlots) */
    /*
    fun getCourseNodesByDay(dayOfWeek: Int): List<CourseNode> {
        // 直接从 weekCourses 的当前值过滤，无需额外 Flow
        return weekCourses.value.flatMap { course ->
            course.nodes.filter { node -> node.day == dayOfWeek }
        }
    }
    */

    // --- 普通日程操作 ---

    /** 添加普通日程 */
    fun addOrdinarySchedule(schedule: OrdinarySchedule) {
        viewModelScope.launch {
            try {
                addOrdinaryScheduleUseCase(schedule)
                // 可以添加成功提示或状态更新
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "Error adding ordinary schedule", e)
                // 处理错误，例如显示错误消息
            }
        }
    }

    /** 更新普通日程 */
    fun updateOrdinarySchedule(schedule: OrdinarySchedule) {
        viewModelScope.launch {
            try {
                updateOrdinaryScheduleUseCase(schedule)
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "Error updating ordinary schedule", e)
            }
        }
    }

    /** 删除普通日程 */
    fun deleteOrdinarySchedule(schedule: OrdinarySchedule) {
        viewModelScope.launch {
            try {
                deleteOrdinaryScheduleUseCase(schedule)
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "Error deleting ordinary schedule", e)
            }
        }
    }

    // --- 辅助函数 ---
    /**
     * 将课程节点列表转换为当前周的 TimeSlot 列表。
     */
    private fun convertCourseNodesToTimeSlots(
        courses: List<Course>,
        week: Int,
        tableTimeConfig: TableTimeConfig,
        table: Table
    ): List<TimeSlot> {
        Log.d(
            "ScheduleViewModel",
            "convertCourseNodesToTimeSlots START: week=$week, configId=${tableTimeConfig.id}, tableId=${table.id}, coursesInputCount=${courses.size}"
        )

        val tableStartDate = table.startDate
        val timeConfigMap =
            tableTimeConfig.nodes.associate { it.node to (it.startTime to it.endTime) }
        Log.d("ScheduleViewModel", "TimeConfigMap Keys: ${timeConfigMap.keys}")
        if (timeConfigMap.isEmpty()) {
            Log.w("ScheduleViewModel", "TimeConfigMap is empty!")
            return emptyList()
        }

        val firstDayOfWeekTableStarts = tableStartDate.dayOfWeek.isoDayNumber
        val firstMondayOfTable =
            tableStartDate.minus(DatePeriod(days = firstDayOfWeekTableStarts - 1))
        val weekStartLocalDate = firstMondayOfTable.plus(DatePeriod(days = (week - 1) * 7))

        val resultSlots = mutableListOf<TimeSlot>()

        courses.forEach { course ->
            Log.d(
                "ScheduleViewModel",
                "Processing Course: id=${course.id}, name=${course.courseName}"
            )
            course.nodes.forEach { node ->
                if (!node.isInWeek(week)) {
                    return@forEach
                }

                val startNodeNum = node.startNode
                val endNodeNum = node.startNode + node.step - 1
                Log.d(
                    "ScheduleViewModel",
                    "Processing Node in Week: startNode=${startNodeNum}, endNode=${endNodeNum}, step=${node.step}, day=${node.day}"
                )

                val nodeStartTimeInfo = timeConfigMap[startNodeNum]
                val nodeEndTimeInfo = timeConfigMap[endNodeNum]
                Log.d(
                    "ScheduleViewModel",
                    "TimeConfig Lookup: startNodeKey=${startNodeNum}, endNodeKey=${endNodeNum}, startTimeFound=${nodeStartTimeInfo != null}, endTimeFound=${nodeEndTimeInfo != null}"
                )

                if (nodeStartTimeInfo != null && nodeEndTimeInfo != null) {
                    val nodeDate = weekStartLocalDate.plus(DatePeriod(days = node.day - 1))
                    val nodeStartDateTime = nodeDate.atTime(nodeStartTimeInfo.first)
                    val nodeEndDateTime = nodeDate.atTime(nodeEndTimeInfo.second)
                    val startMillis = nodeStartDateTime.toInstant(TimeZone.currentSystemDefault())
                        .toEpochMilliseconds()
                    val endMillis = nodeEndDateTime.toInstant(TimeZone.currentSystemDefault())
                        .toEpochMilliseconds()

                    resultSlots.add(
                        TimeSlot(
                            userId = table.userId,
                            startTime = startMillis,
                            endTime = endMillis,
                            scheduleType = ScheduleType.COURSE,
                            scheduleId = course.id,
                            isRepeated = true,
                            displayTitle = course.courseName,
                            displaySubtitle = node.room,
                            displayColor = course.color,
                            head = course.courseName,
                        )
                    )
                    Log.d(
                        "ScheduleViewModel",
                        "--> Successfully converted node (startNode=${startNodeNum}, day=${node.day}) to TimeSlot"
                    )
                } else {
                    Log.w(
                        "ViewModelHelper",
                        "Could not find start or end time for node (start ${startNodeNum}, end ${endNodeNum}) in time config ${tableTimeConfig.id}"
                    )
                }
            }
        }
        Log.d(
            "ScheduleViewModel",
            "convertCourseNodesToTimeSlots END: resultSlotsCount=${resultSlots.size}"
        )
        return resultSlots
    }
}
