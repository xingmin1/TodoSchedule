package com.example.todoschedule.ui.schedule

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.model.TableTimeConfig
import com.example.todoschedule.domain.model.TimeSlot
import com.example.todoschedule.domain.model.findTableForDate
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
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
    //private val _currentWeek = MutableStateFlow(1)
    //val currentWeek: StateFlow<Int> = _currentWeek

    // 当前登录用户 ID 状态
    private val currentUserIdState: StateFlow<Long?> = sessionRepository.currentUserIdFlow

    // 当前默认课表ID状态
    private val _defaultTableIdState: StateFlow<List<Int>> =
        currentUserIdState.flatMapLatest { userId ->
            if (userId != null && userId != -1L) {
                globalSettingRepository.getDefaultTableIds(userId.toInt())
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val defaultTableIdState: StateFlow<List<Int>> = _defaultTableIdState

    // 当前课表状态列表 (基于默认ID列表)
    private val _currentTableState: StateFlow<List<Table?>> =
        _defaultTableIdState.flatMapLatest { tableIds ->
            if (tableIds.isEmpty()) {
                flowOf(emptyList())
            } else {
                // 为每个ID获取课表，然后组合成列表
                val tableFlows = tableIds.map { tableId ->
                    tableRepository.getTableById(tableId)
                }
                combine(tableFlows) { tablesArray ->
                    tablesArray.toList()
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val currentTablesState: StateFlow<List<Table?>> = _currentTableState

    // 获取当前课表的时间配置
    private val currentTablesTimeConfig: StateFlow<List<TableTimeConfig?>> =
        _defaultTableIdState.flatMapLatest { tableIds ->
            if (tableIds.isEmpty()){
                flowOf(emptyList())
            } else {
                val configFlows = tableIds.map { tableId ->
                    getDefaultTableTimeConfigUseCase(tableId)
                        .map { config ->
                            Log.d(
                                "ScheduleViewModel",
                                "Received time config for table $tableId: ${config != null}"
                            )
                            config
                        }
                }
                combine(configFlows) { configsArray ->
                    configsArray.toList()
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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
        _currentTableState.filterNotNull().map{ it.filterNotNull()}, //确保列表内也是非空
        currentTablesTimeConfig.filterNotNull().map{ it.filterNotNull()} //确保列表内也是非空
    ) { userId, tables, configs ->
        tables.mapNotNull { table ->
            if (table.userId == userId.toInt()) {
                val config = configs.find { it.tableId == table.id } // 找到对应的config
                if (config != null) {
                    Triple(userId, table, config)
                } else {
                    Log.e(
                        "ScheduleViewModel",
                        "Config not found for table ${table.id}"
                    )
                    null
                }
            } else {
                Log.e(
                    "ScheduleViewModel",
                    "User ID mismatch between session (${userId}) and table (${table.userId})"
                )
                null
            }
        }
    }.filterNotNull() // 过滤掉不一致或config找不到的情况

    // UI状态 - 声明式定义
    val uiState: StateFlow<ScheduleUiState> = combine(
        currentUserIdState,
        _defaultTableIdState,
        _currentTableState, // 使用列表
        currentTablesTimeConfig // 使用列表
    ) { userId, tableIds, tables, tableTimeConfigs ->
        Log.d(
            "ScheduleViewModel",
            "State combine for UI: userId=$userId, tableIds=$tableIds, tablesCount=${tables.count { it!=null }}, configsCount=${tableTimeConfigs.count{it!=null}}"
        )
        when {
            userId == null && currentUserIdState.value == null -> ScheduleUiState.Loading
            userId == null || userId == -1L -> ScheduleUiState.NoTableSelected
            tableIds.isEmpty() -> ScheduleUiState.NoTableSelected // 没有默认课表ID
            // 检查是否所有请求的课表和配置都已加载（或至少尝试过）
            // 这里简化逻辑：如果ID存在，但对应的table或config是null（且列表大小匹配），则认为是加载中
            // 更稳健的可能是检查是否有任何一个ID对应的table/config仍在加载（需要更复杂的状态跟踪）
            tables.any { it == null && tableIds.contains(it?.id) } || tableTimeConfigs.any { it == null && tableIds.contains(it?.tableId) } && tables.size == tableIds.size && tableTimeConfigs.size == tableIds.size -> {
                 Log.d("ScheduleViewModel", "UI State: Loading table/config details for tableIds $tableIds")
                 ScheduleUiState.Loading
            }
            tables.all { it != null } && tableTimeConfigs.all { it != null } && tables.isNotEmpty() && tableTimeConfigs.isNotEmpty() -> {
                Log.d(
                    "ScheduleViewModel",
                    "UI State: Success - UserID: $userId, Tables loaded: ${tables.mapNotNull { it?.id }}, Configs loaded for tables: ${tableTimeConfigs.mapNotNull { it?.tableId }}"
                )
                ScheduleUiState.Success
            }
            else -> { // 其他中间状态或部分加载完成的状态，也视为Loading或根据需求调整
                Log.d("ScheduleViewModel", "UI State: Fallback to Loading or specific error needed. tables: ${tables.map{it?.id}}, configs: ${tableTimeConfigs.map{it?.tableId}}")
                ScheduleUiState.Loading // Fallback, ideally have more granular states
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScheduleUiState.Loading
    )

    // 当前周的课程列表
    // val weekCourses: StateFlow<List<Course>> = combine(
    //     _defaultTableIdState,
    //     _currentWeek
    // ) { tableId, week ->
    //     tableId to week
    // }.flatMapLatest { (tableId, week) ->
    //     if (tableId <= 0) {
    //         Log.d("ScheduleViewModel", "weekCourses - Invalid tableId: $tableId")
    //         flowOf(emptyList())
    //     } else {
    //         Log.d(
    //             "ScheduleViewModel",
    //             "weekCourses - Fetching courses for tableId: $tableId, week: $week"
    //         )
    //         courseRepository.getCoursesByTableId(tableId).map { courses ->
    //             Log.d(
    //                 "ScheduleViewModel",
    //                 "weekCourses - Original courses for table $tableId: ${courses.size}"
    //             )
    //             courses.map { course ->
    //                 course.copy(
    //                     nodes = course.nodes.filter { node -> node.isInWeek(week) }
    //                 )
    //             }.filter { it.nodes.isNotEmpty() }
    //             .also {
    //                 Log.d(
    //                     "ScheduleViewModel",
    //                     "weekCourses - Filtered courses for week $week: ${it.size}"
    //         }
    //     }
    // }.stateIn(
    //     scope = viewModelScope,
    //     started = SharingStarted.WhileSubscribed(5000),
    //     initialValue = emptyList()
    // )

    // 合并后的、用于UI显示的时间项列表 (只包含当前周)
//    val displayableTimeSlots: StateFlow<List<TimeSlot>> = combine(
//        ordinarySchedules,       // Flow 1: List<OrdinarySchedule> (已关联用户)
//        weekCourses,             // Flow 2: List<Course> (已根据 _currentWeek 过滤)
//        userTableDataFlow,       // Flow 3: Triple<Long, Table, TableTimeConfig>
//        _currentWeek             // Flow 4: Int (用于过滤 ordinarySchedules)
//    ) { schedules, courses, userTableData, week ->
//
//        val (userId, table, tableTimeConfig) = userTableData // 解构 Triple
//
//        Log.d(
//            "ScheduleViewModel",
//            "Combining displayableTimeSlots: userId=$userId, week=$week, tableId=${table.id}, coursesCount=${courses.size}, schedulesCount=${schedules.size}"
//        )
//
//        // 1. 过滤普通日程 TimeSlot 到当前周 (使用 week 参数)
//        val tableStartDate = table.startDate
//        val firstDayOfWeekTableStarts = tableStartDate.dayOfWeek.isoDayNumber
//        val firstMondayOfTable =
//            tableStartDate.minus(DatePeriod(days = firstDayOfWeekTableStarts - 1))
//        val weekStartLocalDate = firstMondayOfTable.plus(DatePeriod(days = (week - 1) * 7))
//        val nextWeekStartLocalDate = weekStartLocalDate.plus(DatePeriod(days = 7))
//        val weekStartInstant =
//            weekStartLocalDate.atTime(0, 0).toInstant(TimeZone.currentSystemDefault())
//        val weekEndInstantExclusive =
//            nextWeekStartLocalDate.atTime(0, 0).toInstant(TimeZone.currentSystemDefault())
//        val weekStartMillis = weekStartInstant.toEpochMilliseconds()
//        val weekEndMillisExclusive = weekEndInstantExclusive.toEpochMilliseconds()
//
//        val ordinaryTimeSlotsInWeek = schedules.flatMap { schedule ->
//            schedule.timeSlots.filter { slot ->
//                (slot.startTime < weekEndMillisExclusive && slot.endTime > weekStartMillis)
//            }.map { slot ->
//                slot.copy(
//                    displayTitle = slot.head ?: schedule.title,
//                    displaySubtitle = schedule.location,
//                    displayColor = schedule.color ?: AppConstants.DEFAULT_COURSE_COLOR
//                )
//            }
//        }
//        Log.d(
//            "ScheduleViewModel",
//            "Ordinary time slots in week $week: ${ordinaryTimeSlotsInWeek.size}"
//        )
//
//        // 2. 将 CourseNode 转换为 TimeSlot (weekCourses 已被过滤)
//        val courseTimeSlotsInWeek =
//            convertCourseNodesToTimeSlots(courses, week, tableTimeConfig, table, userId.toInt())
//        Log.d("ScheduleViewModel", "Course time slots in week $week: ${courseTimeSlotsInWeek.size}")
//        Log.d("ScheduleViewModel", courseTimeSlotsInWeek.toString())
//
//        // 3. 合并列表并排序 (可选)
//        (ordinaryTimeSlotsInWeek + courseTimeSlotsInWeek).sortedBy { it.startTime }
//
//    }.stateIn(
//        scope = viewModelScope,
//        started = SharingStarted.WhileSubscribed(5000),
//        initialValue = emptyList()
//    )

    // 课表视图模式状态，默认周视图
    private val _viewMode = MutableStateFlow(ScheduleViewMode.WEEK)
    val viewMode = _viewMode.asStateFlow()

    // _currentDayDate 现在是日视图和周视图的"锚点日期"
    // 对于日视图，它就是当天日期
    // 对于周视图，它将是该周的某一天（例如周一，或用户通过左右滑动选择的任何一天的对应周一）
    // 对于月视图，它是当月的第一天或用户选择的月份
    private val _anchorDate = MutableStateFlow(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
    val anchorDate: StateFlow<LocalDate> = _anchorDate

    // ================== 新增：以日期为主的连续周视图支持 ==================

    // 当前周的起始日期（周一），默认本周一
    private val _currentWeekStartDate = MutableStateFlow(getThisMonday())
    val currentWeekStartDate: StateFlow<LocalDate> = _currentWeekStartDate

    // 新增: 当前周次 (数字)，用于UI显示 "第X周"
//    val currentWeekNumber: StateFlow<Int> = combine(
//        _currentWeekStartDate,
//        _currentTableState.map { tables -> tables.firstOrNull { it != null } } // 获取第一个有效课表作为周次计算的参考
//    ) { displayedMonday, referenceTable ->
//        if (referenceTable == null) {
//            1 // 如果没有参考课表，默认为第1周
//        } else {
//            // 假设 CalendarUtils 有一个方法可以计算日期相对于课表起始日的周次
//            // 例如: CalendarUtils.getWeekNumber(dateToCalculate: LocalDate, tableStartDate: LocalDate)
//            CalendarUtils.getWeekNumber(displayedMonday, referenceTable.startDate)
//        }
//    }.stateIn(
//        scope = viewModelScope,
//        started = SharingStarted.WhileSubscribed(5000),
//        initialValue = 1 // 初始默认周次
//    )

    // 当前周的7天日期列表
    val currentWeekDates: StateFlow<List<LocalDate>> = _currentWeekStartDate.map { start ->
        (0..6).map { start.plus(DatePeriod(days = it)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = (0..6).map { getThisMonday().plus(DatePeriod(days = it)) }
    )

    /**
     * 辅助函数：为指定课表的所有课程节点生成 TimeSlot 列表 (覆盖所有周)。
     */
    private fun generateAllTimeSlotsForCourseNodes(
        userId: Int,
        courses: List<Course>,
        tableTimeConfig: TableTimeConfig,
        table: Table
    ): List<TimeSlot> {
        val timeConfigMap =
            tableTimeConfig.nodes.associate { it.node to (it.startTime to it.endTime) }
        if (timeConfigMap.isEmpty()) {
            Log.w(
                "ScheduleViewModel",
                "generateAllTimeSlotsForCourseNodes: TimeConfigMap is empty for table ${table.id}"
            )
            return emptyList()
        }

        val tableStartDate = table.startDate
        val firstDayOfWeekTableStarts = tableStartDate.dayOfWeek.isoDayNumber
        val firstMondayOfTable =
            tableStartDate.minus(DatePeriod(days = firstDayOfWeekTableStarts - 1))

        return courses.flatMap { course ->
            course.nodes.flatMap { node ->
                (node.startWeek..node.endWeek).mapNotNull { weekNum -> // weekNum is the integer week number
                    val isValidWeekType = when (node.weekType) {
                        AppConstants.WeekTypes.ALL -> true
                        AppConstants.WeekTypes.ODD -> weekNum % 2 != 0
                        AppConstants.WeekTypes.EVEN -> weekNum % 2 == 0
                        else -> false
                    }
                    if (!isValidWeekType) {
                        return@mapNotNull null
                    }

                    val weekStartLocalDateForNode =
                        firstMondayOfTable.plus(DatePeriod(days = (weekNum - 1) * 7))
                    val nodeDate =
                        weekStartLocalDateForNode.plus(DatePeriod(days = node.day - 1))

                    val startNodeNum = node.startNode
                    val endNodeNum = node.startNode + node.step - 1

                    val nodeStartTimeInfo = timeConfigMap[startNodeNum]
                    val nodeEndTimeInfo = timeConfigMap[endNodeNum]

                    if (nodeStartTimeInfo != null && nodeEndTimeInfo != null) {
                        val nodeStartDateTime = nodeDate.atTime(nodeStartTimeInfo.first)
                        val nodeEndDateTime = nodeDate.atTime(nodeEndTimeInfo.second)
                        val startMillis =
                            nodeStartDateTime.toInstant(TimeZone.currentSystemDefault())
                                .toEpochMilliseconds()
                        val endMillis =
                            nodeEndDateTime.toInstant(TimeZone.currentSystemDefault())
                                .toEpochMilliseconds()

                        TimeSlot(
                            userId = userId,
                            startTime = startMillis,
                            endTime = endMillis,
                            scheduleType = ScheduleType.COURSE,
                            scheduleId = course.id,
                            isRepeated = true,
                            displayTitle = course.courseName,
                            displaySubtitle = node.room,
                            displayColor = course.color,
                            head = course.courseName
                        )
                    } else {
                        Log.w(
                            "ScheduleViewModel",
                            "generateAllTimeSlotsForCourseNodes: Time config not found for node $startNodeNum or $endNodeNum in table ${table.id}, config ${tableTimeConfig.id}"
                        )
                        null
                    }
                }
            }
        }
    }

    // 所有日程（课程+普通日程），便于任意日期范围筛选
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    val allTimeSlots: StateFlow<List<TimeSlot>> = combine(
        ordinarySchedules, 
        _currentTableState.flatMapLatest { tablesList -> 
            val validTables = tablesList.filterNotNull()
            if (validTables.isEmpty()) {
                flowOf(emptyList<TimeSlot>())
            } else {
                val flowsOfTimeSlotsPerTable: List<Flow<List<TimeSlot>>> =
                    validTables.map { table ->
                        combine(
                            courseRepository.getCoursesByTableId(table.id),
                            tableTimeConfigRepository.getDefaultTimeConfig(table.id),
                            currentUserIdState.filterNotNull()
                        ) { coursesForTable, configForTable, userId ->
                            if (configForTable == null) {
                                Log.w("ScheduleViewModel", "allTimeSlots: No time config for table ${table.id}")
                                emptyList<TimeSlot>()
                            } else {
                                generateAllTimeSlotsForCourseNodes(
                                    userId = userId.toInt(),
                                    courses = coursesForTable,
                                    tableTimeConfig = configForTable,
                                    table = table
                                )
                            }
                        }
                    }
                if (flowsOfTimeSlotsPerTable.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(flowsOfTimeSlotsPerTable) { arrayOfListsOfTimeSlots ->
                        arrayOfListsOfTimeSlots.toList().flatten()
                    }
                }
            }
        }
    ) { ordinarySchedulesList, courseTimeSlotsList ->
        val ordinaryTimeSlots = ordinarySchedulesList.flatMap { schedule ->
            schedule.timeSlots.map { slot ->
                slot.copy(
                    displayTitle = slot.head ?: schedule.title,
                    displaySubtitle = schedule.location,
                    displayColor = schedule.color ?: AppConstants.DEFAULT_COURSE_COLOR
                )
            }
        }
        (ordinaryTimeSlots + courseTimeSlotsList).sortedBy { it.startTime }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 当前周的所有日程，按天分组
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    val weekTimeSlotsMap: StateFlow<Map<LocalDate, List<TimeSlot>>> = combine(
        currentWeekDates,
        allTimeSlots
    ) { weekDates, allSlots ->
        weekDates.associateWith { date ->
            allSlots.filter { slot -> slot.isOnDate(date) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // 切换当前周的起始日期（周一）
    fun updateCurrentWeekStartDate(newStart: LocalDate) {
        _currentWeekStartDate.value = newStart
    }

    // 辅助函数：获取本周一
    private fun getThisMonday(): LocalDate {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - 1))
    }

    // 辅助函数：判断TimeSlot是否在某天
    private fun TimeSlot.isOnDate(date: LocalDate): Boolean {
        val slotDate = Instant.fromEpochMilliseconds(this.startTime)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        return slotDate == date
    }

    // --- 多课表支持相关状态 ---
    private val _allTables = MutableStateFlow<List<Table>>(emptyList())
    val allTables: StateFlow<List<Table>> = _allTables

    // 当前系统日期（主要用于自动切换课表时的参考点）
    private val _systemCurrentDate = MutableStateFlow(
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    )

    // 当前日期对应的活动课表 (单个)
    private val _currentActiveTable = MutableStateFlow<Table?>(null)
    val currentActiveTable: StateFlow<Table?> = _currentActiveTable

    /**
     * 根据日期切换当前活动课表
     */
    fun updateCurrentActiveTableForDate(date: LocalDate) {
        // _systemCurrentDate.value = date // No, _systemCurrentDate should be actual system date, _anchorDate is for UI focus
        val tableForDate = findTableForDate(date, _allTables.value)
        if (_currentActiveTable.value?.id != tableForDate?.id) {
            _currentActiveTable.value = tableForDate
            Log.d("ScheduleViewModel", "Current active table set to: ${tableForDate?.tableName} for date $date")
        }
    }

    /**
     * 加载所有课表，并根据当前系统日期自动切换当前活动课表
     */
    private fun loadAllTablesAndSetActive() {
        viewModelScope.launch {
            tableRepository.getAllTables().collect { tables ->
                _allTables.value = tables
                updateCurrentActiveTableForDate(_systemCurrentDate.value) // Use system date for initial active table
            }
        }
    }

    init {
        Log.d("ScheduleViewModel", "ViewModel initialized")

        // 当默认课表列表中的第一个课表加载/变化时，更新 currentWeekStartDate
        // 以便周视图能对齐到该课表的当前实际周
        viewModelScope.launch {
            _currentTableState
                .mapNotNull { tables -> tables.firstOrNull { it != null } } // Get the first valid table from the list
                .distinctUntilChanged { old, new -> old.id == new.id && old.startDate == new.startDate }
                .collect { firstTable ->
                    // Calculate the Monday of the current actual week relative to this table's start date
                    val currentActualWeekNumberForTable = CalendarUtils.getCurrentWeek(firstTable.startDate)
                    // Assuming getWeekDates returns a list where the first element is Monday
                    val mondayOfActualWeekForTable = CalendarUtils.getWeekDates(currentActualWeekNumberForTable, firstTable.startDate).firstOrNull()

                    if (mondayOfActualWeekForTable != null && _currentWeekStartDate.value != mondayOfActualWeekForTable) {
                        Log.d(
                            "ScheduleViewModel",
                            "Aligning week view: CurrentWeekStartDate set to $mondayOfActualWeekForTable based on table '${firstTable.tableName}' (ID: ${firstTable.id}) starting ${firstTable.startDate}"
                        )
                        _currentWeekStartDate.value = mondayOfActualWeekForTable
                    }
                }
        }

        // 检查并确保所有默认课表的默认时间配置都存在
        ensureDefaultTimeConfigExists()

        // 初始化时加载所有课表，并设置当前活动课表
        loadAllTablesAndSetActive()
        // 监听所有课表列表变化，以防外部修改课表后能自动更新活动课表
        viewModelScope.launch {
            _allTables.collect { 
                updateCurrentActiveTableForDate(_anchorDate.value) // Update based on current UI anchor if all tables change
            }
        }
        // When anchorDate changes (e.g. user swipes in Week/Day view), update active table indicator
        viewModelScope.launch {
            _anchorDate.collect {
                updateCurrentActiveTableForDate(it)
            }
        }
    }

    private fun ensureDefaultTimeConfigExists() {
        viewModelScope.launch {
            _defaultTableIdState.collect { tableIds ->
                tableIds.forEach { tableId ->
                    launch { 
                        val table = tableRepository.fetchTableById(tableId)
                        val config = tableTimeConfigRepository.getDefaultTimeConfig(tableId).firstOrNull()

                        if (table != null && config == null && table.id != AppConstants.Ids.INVALID_TABLE_ID) {
                            Log.w(
                                "ScheduleViewModel",
                                "Default time config not found for table ${table.id}. Attempting to create one."
                            )
                            try {
                                currentUserIdState.value?.let { userId ->
                                    tableTimeConfigRepository.ensureDefaultTimeConfig(table.id, userId.toInt())
                                    Log.i(
                                        "ScheduleViewModel",
                                        "ensureDefaultTimeConfig called for table ${table.id}, userId $userId."
                                    )
                                } ?: Log.e("ScheduleViewModel", "Cannot ensure default time config, userId is null.")
                            } catch (e: Exception) {
                                Log.e(
                                    "ScheduleViewModel",
                                    "Error ensuring default time config for table ${table.id}",
                                    e
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** 设置UI锚点日期，例如用户在日历控件选择某天，或周视图/日视图滑动到某天 */
    fun setAnchorDate(date: LocalDate) {
        _anchorDate.value = date
        // currentActiveTable will update via the collect on _anchorDate
    }

    /** 跳转到今天的逻辑，会更新锚点日期 */
    fun goToToday() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        setAnchorDate(today)
        // Specific views (Day, Week, Month) will observe anchorDate and adjust their display.
        // For example, WeekView might set its displayed 7 days based on this anchor (e.g., Monday of the week of anchorDate).
        // MonthView might scroll to the month of anchorDate.
        // DayView will show anchorDate.
    }

    /** 切换课表视图模式 */
    fun setViewMode(mode: ScheduleViewMode) {
        if (_viewMode.value != mode) {
            _viewMode.value = mode
        }
    }
}

// CourseNode.isInWeek(week: Int) is still needed by generateAllTimeSlotsForCourseNodes if it uses integer week numbers
// This might need to be refactored if the CourseNode model itself changes how week validity is checked.
private fun CourseNode.isInWeek(weekNum: Int): Boolean {
    return weekNum in startWeek..endWeek &&
            (weekType == 0 ||
             (weekType == AppConstants.WeekTypes.ODD && weekNum % 2 != 0) ||
             (weekType == AppConstants.WeekTypes.EVEN && weekNum % 2 == 0))
}
