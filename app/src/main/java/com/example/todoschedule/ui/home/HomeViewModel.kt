package com.example.todoschedule.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.converter.ScheduleStatus
import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.model.TableTimeConfig
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.domain.use_case.ordinary_schedule.GetOrdinarySchedulesUseCase
import com.example.todoschedule.domain.use_case.ordinary_schedule.UpdateOrdinaryScheduleUseCase
import com.example.todoschedule.domain.use_case.table_time_config.GetDefaultTableTimeConfigUseCase
import com.example.todoschedule.domain.utils.CalendarUtils
import com.example.todoschedule.ui.home.model.HomeUiState
import com.example.todoschedule.ui.ordinaryschedule.formatTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    private val courseRepository: CourseRepository,
    private val tableRepository: TableRepository,
    private val globalSettingRepository: GlobalSettingRepository,
    private val getOrdinarySchedulesUseCase: GetOrdinarySchedulesUseCase,
    private val updateOrdinarySchedulesUseCase: UpdateOrdinaryScheduleUseCase,
    private val getDefaultTableTimeConfigUseCase: GetDefaultTableTimeConfigUseCase
) : ViewModel() {

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

    // 获取今天的日期
    private val _todayDate = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    val todayDate = _todayDate

    // 获取今天的周次
    private val _todayWeek: StateFlow<Int> = currentTableState.map { table ->
        table?.startDate?.let { startDate ->
            CalendarUtils.getCurrentWeek(startDate)
        } ?: 1
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1
    )
    val todayWeek: StateFlow<Int> = _todayWeek

    // 获取当前课表时间配置
    private val _currentTableTimeConfig: StateFlow<TableTimeConfig?> =
        _defaultTableIdState.flatMapLatest { tableId ->
            if (tableId != AppConstants.Ids.INVALID_TABLE_ID) {
                Log.d("HomeViewModel", "Fetching time config for tableId: $tableId")
                getDefaultTableTimeConfigUseCase(tableId)
                    .map { config ->
                        Log.d(
                            "HomeViewModel",
                            "Received time config for table $tableId: ${config != null}"
                        )
                        config
                    }
            } else {
                Log.d(
                    "HomeViewModel",
                    "Invalid tableId ($tableId), setting time config to null"
                )
                flowOf(null)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    val currentTableTimeConfig: StateFlow<TableTimeConfig?> = _currentTableTimeConfig

    data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    // 获取今天的课程列表
    private val _todayCourses: StateFlow<List<HomeCourseUiModel>> = combine(
        defaultTableIdState,
        todayWeek,
        todayDate,
        currentTableTimeConfig
    ) { tableId, week, date, timeConfig ->
        Quadruple(tableId, week, date, timeConfig)
    }.flatMapLatest { (tableId, week, date, timeConfig) ->
        if (tableId <= 0) {
            flowOf(emptyList())
        } else {
            courseRepository.getCoursesByTableId(tableId).map { courses ->
                val dayOfWeek = date.dayOfWeek.isoDayNumber // 1=Monday, 7=Sunday
                courses.flatMap { course ->
                    course.nodes.filter { node ->
                        node.isInWeek(week) && node.day == dayOfWeek
                    }.map { node ->
                        val startTimeNode = timeConfig?.nodes?.find { it.node == node.startNode }
                        val endTimeNode =
                            timeConfig?.nodes?.find { it.node == node.startNode + node.step - 1 }
                        val timeDisplay = if (startTimeNode != null && endTimeNode != null) {
                            "${startTimeNode.startTime.formatTime()} - ${endTimeNode.endTime.formatTime()}"
                        } else {
                            "第${node.startNode} - ${node.startNode + node.step - 1}节"
                        }
                        HomeCourseUiModel(
                            id = course.id,
                            name = course.courseName,
                            startNode = node.startNode,
                            endNode = node.startNode + node.step - 1,
                            location = node.room ?: "",
                            timeDisplay = timeDisplay
                        )
                    }
                }.sortedBy { it.startNode } // 按开始节数排序
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val todayCourses: StateFlow<List<HomeCourseUiModel>> = _todayCourses

    // 获取今日待办列表
    private val _todayOrdinarySchedules: StateFlow<List<HomeScheduleUiModel>> = combine(
        ordinarySchedules,
        todayDate
    ) { schedules, date ->
        // 今天
        val startOfDay =
            date.atTime(0, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val endOfDay = date.atTime(23, 59, 59, 999).toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        // 明天
        val tomorrow = date.plus(1, DateTimeUnit.DAY)
        val startOfTomorrow =
            tomorrow.atTime(0, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val endOfTomorrow =
            tomorrow.atTime(23, 59, 59, 999).toInstant(TimeZone.currentSystemDefault())
                .toEpochMilliseconds()

        schedules.flatMap { schedule ->
            schedule.timeSlots
                .filter { slot ->
                    (slot.startTime in startOfDay..endOfDay) ||
                            (slot.endTime in startOfDay..endOfDay) ||
                            (slot.startTime < startOfDay && slot.endTime > endOfDay)
                }
                .filter { slot ->
                    schedule.status == ScheduleStatus.TODO || schedule.status == ScheduleStatus.IN_PROGRESS
                }
                .map { slot ->
                    // 格式化输出
                    val timeDisplay = when (slot.endTime) {
                        in startOfDay..endOfDay -> {
                            val time = Instant.fromEpochMilliseconds(slot.endTime)
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                            "截止时间: 今日${
                                time.hour.toString().padStart(2, '0')
                            }:${time.minute.toString().padStart(2, '0')}"
                        }

                        in startOfTomorrow..endOfTomorrow -> {
                            "截止时间: 明日"
                        }

                        else -> {
                            val date = Instant.fromEpochMilliseconds(slot.endTime)
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                            "截止时间: ${date.year}/${
                                date.monthNumber.toString().padStart(2, '0')
                            }/${date.dayOfMonth.toString().padStart(2, '0')}"
                        }
                    }

                    HomeScheduleUiModel(
                        id = schedule.id,
                        title = schedule.title,
                        description = schedule.description,
                        status = schedule.status,
                        timeDisplay = timeDisplay,
                        priority = slot.priority
                    )
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val todayOrdinarySchedules: StateFlow<List<HomeScheduleUiModel>> = _todayOrdinarySchedules

    // UI状态
    val uiState: StateFlow<HomeUiState> = combine(
        currentUserIdState,
        defaultTableIdState,
        currentTableState,
        todayCourses,
        todayOrdinarySchedules
    ) { userId, tableId, table, courses, ordinarySchedules ->
        Log.d(
            "HomeViewModel",
            "State combine for UI: userId=$userId, tableId=$tableId, table=${table?.id}, coursesCount=${courses.size}, ordinaryCount=${ordinarySchedules.size}"
        )
        when {
            // 1. 等待 userId 确定 (初始状态)
            userId == null && currentUserIdState.value == null -> {
                HomeUiState.Loading
            }
            // 2. 用户未登录 (userId 最终确定为 null 或 -1L)
            userId == null || userId == -1L -> {
                HomeUiState.NoTableSelected
            }
            // 3. 已登录，但没有有效的默认课表 ID (tableId 加载完成但值为 -1)
            tableId == AppConstants.Ids.INVALID_TABLE_ID -> {
                HomeUiState.NoTableSelected
            }
            // 4. 有有效 tableId，但课表详细信息仍在加载
            table == null -> {
                HomeUiState.Loading
            }
            // 5. 成功加载所有必要数据
            else -> {
                HomeUiState.Success
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Loading
    )

    init {
        Log.d("HomeViewModel", "ViewModel initialized")
    }

    fun toggleComplete(taskItem: HomeScheduleUiModel) {
        viewModelScope.launch {

            try {
                val userId = currentUserIdState.value?.toInt() ?: -1
                if (userId == -1) return@launch

                val schedules = getOrdinarySchedulesUseCase(userId).firstOrNull() ?: return@launch

                val scheduleToUpdate = schedules.find { it.id == taskItem.id } ?: return@launch

                val updatedSchedule = scheduleToUpdate.copy(
                    status = ScheduleStatus.DONE
                )

                updateOrdinarySchedulesUseCase(updatedSchedule)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error toggling task complete", e)
            }
        }
    }

}