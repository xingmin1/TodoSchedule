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
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.domain.repository.TableTimeConfigRepository
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
    sessionRepository: SessionRepository,
    private val tableRepository: TableRepository,
    private val globalSettingRepository: GlobalSettingRepository,
    getOrdinarySchedulesUseCase: GetOrdinarySchedulesUseCase,
    private val addOrdinaryScheduleUseCase: AddOrdinaryScheduleUseCase,
    private val updateOrdinaryScheduleUseCase: UpdateOrdinaryScheduleUseCase,
    private val deleteOrdinaryScheduleUseCase: DeleteOrdinaryScheduleUseCase,
    private val getDefaultTableTimeConfigUseCase: GetDefaultTableTimeConfigUseCase,
    private val tableTimeConfigRepository: TableTimeConfigRepository
) : ViewModel() {

    // 当前选择的周次
    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek

    // 当前登录用户 ID 状态
    private val currentUserIdState: StateFlow<Long?> = sessionRepository.currentUserIdFlow

    // 当前默认课表ID状态
    private val _defaultTableIdState: StateFlow<Int> = currentUserIdState.flatMapLatest { userId ->
        if (userId != null && userId != -1L) {
            globalSettingRepository.getDefaultTableIds(userId.toInt())
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

    // 当前周的日期列表 - 改为响应式 Flow
    val weekDates: StateFlow<List<LocalDate>> = combine(
        _currentWeek,
        currentTableState.map { it?.startDate } // 只关心 startDate
    ) { week, startDate ->
        Log.d("ScheduleViewModel", "Calculating week dates for week $week, startDate $startDate")
        CalendarUtils.getWeekDates(week, startDate) // 直接调用工具类计算
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList() // 初始值为空列表
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

    // 获取当前登录用户的普通日程
    private val ordinarySchedules: StateFlow<List<OrdinarySchedule>> =
        currentUserIdState.flatMapLatest { userId ->
            if (userId != null && userId != -1L) {
                getOrdinarySchedulesUseCase(userId.toInt())
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- 新增: 组合用户相关的课表数据 ---
    private val userTableDataFlow = combine(
        currentUserIdState.filterNotNull(),
        currentTableState.filterNotNull(),
        currentTableTimeConfig.filterNotNull()
    ) { userId, table, config ->
        // 可选：添加校验，确保 table.userId 与当前 userId 一致
        if (table.userId == userId.toInt()) {
            Triple(userId, table, config)
        } else {
            Log.e(
                "ScheduleViewModel",
                "User ID mismatch between session (${userId}) and table (${table.userId})"
            )
            null // 如果不一致，则发 null
        }
    }.filterNotNull() // 过滤掉不一致的情况

    // UI状态 - 声明式定义
    val uiState: StateFlow<ScheduleUiState> = combine(
        currentUserIdState,
        _defaultTableIdState,
        currentTableState,
        currentTableTimeConfig
    ) { userId, tableId, table, tableTimeConfig ->
        Log.d(
            "ScheduleViewModel",
            "State combine for UI: userId=$userId, tableId=$tableId, table=${table?.id}, config=${tableTimeConfig != null}"
        )
        when {
            // 1. 等待 userId 确定 (初始状态)
            userId == null && currentUserIdState.value == null -> {
                Log.d("ScheduleViewModel", "UI State: Initial Loading (Waiting for userId)")
                ScheduleUiState.Loading
            }
            // 2. 用户未登录 (userId 最终确定为 null 或 -1L)
            userId == null || userId == -1L -> {
                Log.w("ScheduleViewModel", "UI State: User not logged in.")
                ScheduleUiState.NoTableSelected // 未登录视为无课表状态
            }
            // 3. 已登录，但没有有效的默认课表 ID (tableId 加载完成但值为 -1)
            tableId == AppConstants.Ids.INVALID_TABLE_ID -> {
                Log.w(
                    "ScheduleViewModel",
                    "UI State: No default table selected (tableId is invalid)."
                )
                ScheduleUiState.NoTableSelected
            }
            // 4. 有有效 tableId，但课表或时间配置的详细信息仍在加载
            table == null || tableTimeConfig == null -> {
                Log.d(
                    "ScheduleViewModel",
                    "UI State: Loading table/config details for tableId $tableId"
                )
                ScheduleUiState.Loading
            }
            // 5. 成功加载所有必要数据
            else -> {
                Log.d(
                    "ScheduleViewModel",
                    "UI State: Success - UserID: $userId, Table: ${table.id}, Config: ${tableTimeConfig.id}"
                )
                ScheduleUiState.Success
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScheduleUiState.Loading
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

    // 合并后的、用于UI显示的时间项列表 (只包含当前周)
    val displayableTimeSlots: StateFlow<List<TimeSlot>> = combine(
        ordinarySchedules,       // Flow 1: List<OrdinarySchedule> (已关联用户)
        weekCourses,             // Flow 2: List<Course> (已根据 _currentWeek 过滤)
        userTableDataFlow,       // Flow 3: Triple<Long, Table, TableTimeConfig>
        _currentWeek             // Flow 4: Int (用于过滤 ordinarySchedules)
    ) { schedules, courses, userTableData, week ->

        val (userId, table, tableTimeConfig) = userTableData // 解构 Triple

        Log.d(
            "ScheduleViewModel",
            "Combining displayableTimeSlots: userId=$userId, week=$week, tableId=${table.id}, coursesCount=${courses.size}, schedulesCount=${schedules.size}"
        )

        // 1. 过滤普通日程 TimeSlot 到当前周 (使用 week 参数)
        val tableStartDate = table.startDate
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

        // 2. 将 CourseNode 转换为 TimeSlot (weekCourses 已被过滤)
        val courseTimeSlotsInWeek =
            convertCourseNodesToTimeSlots(courses, week, tableTimeConfig, table, userId.toInt())
        Log.d("ScheduleViewModel", "Course time slots in week $week: ${courseTimeSlotsInWeek.size}")
        Log.d("ScheduleViewModel", courseTimeSlotsInWeek.toString())

        // 3. 合并列表并排序 (可选)
        (ordinaryTimeSlotsInWeek + courseTimeSlotsInWeek).sortedBy { it.startTime }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        Log.d("ScheduleViewModel", "ViewModel initialized")

        // 观察课表状态以初始化当前周 (仅执行一次或当 table 变化时)
        viewModelScope.launch {
            currentTableState
                .filterNotNull() // 只在有有效课表时执行
                .distinctUntilChanged { old, new -> old.id == new.id && old.startDate == new.startDate } // 避免重复计算
                .collect { table ->
                    val startDate = table.startDate
                    val calculatedCurrentWeek = CalendarUtils.getCurrentWeek(startDate)
                    if (_currentWeek.value != calculatedCurrentWeek) {
                        Log.d(
                            "ScheduleViewModel",
                            "Initializing/Updating current week to $calculatedCurrentWeek based on table start date $startDate"
                        )
                        _currentWeek.value = calculatedCurrentWeek
                        // 不再需要手动更新 weekDates，它会通过 combine 自动响应 _currentWeek 的变化
                    }
                }
        }

        // 检查并确保默认时间配置存在
        ensureDefaultTimeConfigExists()
    }

    // 检查并确保默认时间配置存在的函数
    private fun ensureDefaultTimeConfigExists() {
        combine(
            currentTableState,
            currentTableTimeConfig,
            currentUserIdState.filterNotNull()
        ) { table, config, userId ->
            Triple(table, config, userId)
        }
            .distinctUntilChanged()
            .onEach { (table, config, userId) ->
                if (table != null && config == null && table.id != AppConstants.Ids.INVALID_TABLE_ID) {
                    Log.w(
                        "ScheduleViewModel",
                        "Default time config not found for table ${table.id}. Attempting to create one."
                    )
                    try {
                        tableTimeConfigRepository.ensureDefaultTimeConfig(table.id, userId.toInt())
                        Log.i(
                            "ScheduleViewModel",
                            "ensureDefaultTimeConfig called for table ${table.id}, userId ${table.userId}."
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
            .launchIn(viewModelScope)
    }

    // --- UI 事件处理 ---

    /** 更新当前周 */
    fun updateCurrentWeek(week: Int) {
        if (week > 0 && week != _currentWeek.value) {
            Log.d("ScheduleViewModel", "Updating current week to $week")
            _currentWeek.value = week
            // 不再需要手动更新 weekDates
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
        if (newWeek <= CalendarUtils.MAX_WEEKS) { // 假设 CalendarUtils 有 MAX_WEEKS 常量
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

    // --- 普通日程操作 ---

    /** 添加普通日程 */
    fun addOrdinarySchedule(schedule: OrdinarySchedule) {
        viewModelScope.launch {
            val userId = currentUserIdState.value?.toInt()
            if (userId == null) {
                Log.e("ScheduleViewModel", "Cannot add schedule: User not logged in")
                return@launch
            }
            try {
                addOrdinaryScheduleUseCase(schedule.copy(userId = userId))
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "Error adding ordinary schedule", e)
            }
        }
    }

    /** 更新普通日程 */
    fun updateOrdinarySchedule(schedule: OrdinarySchedule) {
        viewModelScope.launch {
            val userId = currentUserIdState.value?.toInt()
            if (userId == null || schedule.userId != userId) {
                Log.e(
                    "ScheduleViewModel",
                    "Cannot update schedule: User not logged in or ID mismatch"
                )
                return@launch
            }
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
            val userId = currentUserIdState.value?.toInt()
            if (userId == null || schedule.userId != userId) {
                Log.e(
                    "ScheduleViewModel",
                    "Cannot delete schedule: User not logged in or ID mismatch"
                )
                return@launch
            }
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
    /**
     * 将课程节点列表转换为当前周的 TimeSlot 列表。
     *
     * @param courses 课程列表
     * @param week 当前周次
     * @param tableTimeConfig 课表时间配置
     * @param table 课表信息
     * @param userId 用户ID
     * @return 转换后的TimeSlot列表
     */
    private fun convertCourseNodesToTimeSlots(
        courses: List<Course>,
        week: Int,
        tableTimeConfig: TableTimeConfig,
        table: Table,
        userId: Int
    ): List<TimeSlot> {
        Log.d(
            "ScheduleViewModel",
            "convertCourseNodesToTimeSlots START: userId=$userId, week=$week, configId=${tableTimeConfig.id}, tableId=${table.id}, coursesInputCount=${courses.size}"
        )

        // 获取课表的起始日期
        val tableStartDate = table.startDate
        // 创建节点到时间区间的映射表，用于快速查找每个节次的开始和结束时间
        val timeConfigMap =
            tableTimeConfig.nodes.associate { it.node to (it.startTime to it.endTime) }
        Log.d("ScheduleViewModel", "TimeConfigMap: ${timeConfigMap}")
        if (timeConfigMap.isEmpty()) {
            Log.w("ScheduleViewModel", "TimeConfigMap is empty!")
            return emptyList()
        }

        // 计算课表开始日期的星期几（ISO标准，1=周一，7=周日）
        val firstDayOfWeekTableStarts = tableStartDate.dayOfWeek.isoDayNumber
        // 计算课表开始的那一周的周一日期
        val firstMondayOfTable =
            tableStartDate.minus(DatePeriod(days = firstDayOfWeekTableStarts - 1))
        // 计算当前选择周的周一日期
        val weekStartLocalDate = firstMondayOfTable.plus(DatePeriod(days = (week - 1) * 7))

        // 用于存储转换后的TimeSlot结果
        val resultSlots = mutableListOf<TimeSlot>()

        // 遍历所有课程
        courses.forEach { course ->
            Log.d(
                "ScheduleViewModel",
                "Processing Course: id=${course.id}, name=${course.courseName}"
            )
            // 遍历每个课程的节点信息
            course.nodes.forEach { node ->
                // 二次检查节点是否在当前周，因为weekCourses可能因缓存等原因包含非本周节点
                if (!node.isInWeek(week)) {
                    return@forEach
                }

                // 计算节点的开始和结束节次
                val startNodeNum = node.startNode
                val endNodeNum = node.startNode + node.step - 1
                Log.d(
                    "ScheduleViewModel",
                    "Processing Node in Week: startNode=${startNodeNum}, endNode=${endNodeNum}, step=${node.step}, day=${node.day}"
                )

                // 查找节点对应的时间信息
                val nodeStartTimeInfo = timeConfigMap[startNodeNum]
                val nodeEndTimeInfo = timeConfigMap[endNodeNum]
                Log.d(
                    "ScheduleViewModel",
                    "TimeConfig Lookup: startNodeKey=${startNodeNum}, endNodeKey=${endNodeNum}, startTimeFound=${nodeStartTimeInfo != null}, endTimeFound=${nodeEndTimeInfo != null}"
                )

                // 只有当开始和结束时间都能找到时才进行转换
                if (nodeStartTimeInfo != null && nodeEndTimeInfo != null) {
                    // 计算课程当天的日期（周一+偏移天数）
                    val nodeDate = weekStartLocalDate.plus(DatePeriod(days = node.day - 1))
                    // 创建课程的开始和结束时间点
                    val nodeStartDateTime = nodeDate.atTime(nodeStartTimeInfo.first)
                    val nodeEndDateTime = nodeDate.atTime(nodeEndTimeInfo.second)
                    // 转换为毫秒时间戳
                    val startMillis = nodeStartDateTime.toInstant(TimeZone.currentSystemDefault())
                        .toEpochMilliseconds()
                    val endMillis = nodeEndDateTime.toInstant(TimeZone.currentSystemDefault())
                        .toEpochMilliseconds()

                    // 创建并添加TimeSlot对象
                    resultSlots.add(
                        TimeSlot(
                            userId = userId,
                            startTime = startMillis,    // 开始时间戳
                            endTime = endMillis,        // 结束时间戳
                            scheduleType = ScheduleType.COURSE,  // 类型为课程
                            scheduleId = course.id,     // 关联的课程ID
                            isRepeated = true,          // 标记为重复事件
                            displayTitle = course.courseName,  // 显示标题
                            displaySubtitle = node.room,       // 显示副标题（教室）
                            displayColor = course.color,       // 显示颜色
                            head = course.courseName,          // 标题
                        )
                    )
                    Log.d(
                        "ScheduleViewModel",
                        "--> Successfully converted node (startNode=${startNodeNum}, day=${node.day}) to TimeSlot"
                    )
                } else {
                    // 记录警告日志，当无法找到节次对应的时间信息
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

// 扩展函数: 检查节点是否在指定周
private fun com.example.todoschedule.domain.model.CourseNode.isInWeek(week: Int): Boolean {
    return week in startWeek..endWeek &&
            (weekType == 0 || (weekType == 1 && week % 2 != 0) || (weekType == 2 && week % 2 == 0))
}
