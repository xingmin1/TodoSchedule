package com.example.todoschedule.ui.task

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.data.database.converter.ScheduleStatus
import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.domain.use_case.ordinary_schedule.GetOrdinarySchedulesUseCase
import com.example.todoschedule.domain.use_case.ordinary_schedule.UpdateOrdinaryScheduleUseCase
import com.example.todoschedule.domain.use_case.table_time_config.GetDefaultTableTimeConfigUseCase
import com.example.todoschedule.domain.utils.CalendarUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
open class TaskViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val getOrdinarySchedulesUseCase: GetOrdinarySchedulesUseCase,
    private val updateOrdinaryScheduleUseCase: UpdateOrdinaryScheduleUseCase,
    private val getDefaultTableTimeConfigUseCase: GetDefaultTableTimeConfigUseCase,
    private val courseRepository: CourseRepository,
    private val globalSettingRepository: GlobalSettingRepository,
    private val tableRepository: TableRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    // 用于组合 Flow
    private val currentUserIdFlow = sessionRepository.currentUserIdFlow
    private val defaultTableIdsFlow = currentUserIdFlow.flatMapLatest {
        it?.let { userId -> globalSettingRepository.getDefaultTableIds(userId.toInt()) } ?: flowOf(
            emptyList()
        )
    }
    private val defaultTableTimeConfig = defaultTableIdsFlow.flatMapLatest {
        getDefaultTableTimeConfigUseCase(it[0])
    }

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                currentUserIdFlow,
                defaultTableIdsFlow,
                _uiState.map { it.selectedFilter }
                    .distinctUntilChanged(), // Only trigger on filter change
                _uiState.map { it.searchTerm }
                    .distinctUntilChanged() // Only trigger on search term change
            ) { userId, defaultTableIds, filter, searchTerm ->
                // Triple containing userId, defaultTableIds, and filter/search info
                Triple(userId, defaultTableIds, Pair(filter, searchTerm))
            }.flatMapLatest { (userId, defaultTableIds, filterAndSearch) ->
                val (filter, searchTerm) = filterAndSearch
                if (userId == null || defaultTableIds.isEmpty()) {
                    // Handle no user or no default table case
                    flowOf(
                        TaskUiState(
                            isLoading = false,
                            tasks = emptyList(),
                            selectedFilter = filter,
                            searchTerm = searchTerm
                        )
                    )
                } else {
                    loadTasks(userId.toInt(), defaultTableIds.first(), filter, searchTerm)
                }
            }.catch { e ->
                Log.e("TaskViewModel", "Error observing data", e)
                // Emit error state
                emit(TaskUiState(isLoading = false, errorMessage = "加载任务失败: ${e.message}"))
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun loadTasks(
        userId: Int,
        defaultTableId: Int,
        filter: TaskFilter,
        searchTerm: String
    ): Flow<TaskUiState> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val currentWeekNumber = CalendarUtils.getCurrentWeek() // TODO: Needs table start date

        return combine(
            defaultTableTimeConfig,
            getOrdinarySchedulesUseCase(userId),
            courseRepository.getCoursesByTableId(defaultTableId),
            tableRepository.getTableById(defaultTableId) // To get start date for week calculation
        ) { tableTimeConfig, schedules, courses, table ->

            _uiState.update { it.copy(isLoading = true) } // Indicate loading start

            val tableStartDate = table?.startDate
                ?: CalendarUtils.getCurrentWeekStart() // Fallback if table not found
            val actualCurrentWeek = CalendarUtils.getCurrentWeek(tableStartDate)
            val weekDates = CalendarUtils.getWeekDates(actualCurrentWeek, tableStartDate)
            val weekStart = weekDates.first()
            val weekEnd = weekDates.last()

            val allTasks = mutableListOf<TaskItemUiModel>()

            // --- Process Ordinary Schedules ---
            schedules.forEach { schedule ->
                schedule.timeSlots.forEach { slot ->
                    val startTime = Instant.fromEpochMilliseconds(slot.startTime)
                    val endTime = Instant.fromEpochMilliseconds(slot.endTime)
                    val startTimeLocal = startTime.toLocalDateTime(TimeZone.currentSystemDefault())

                    val taskDate = startTimeLocal.date
                    val isToday = taskDate == today
                    val isThisWeek = taskDate in weekStart..weekEnd
                    val isCompleted = schedule.status == ScheduleStatus.DONE

                    // Filtering logic
                    val matchesFilter = when (filter) {
                        TaskFilter.ALL -> !isCompleted
                        TaskFilter.TODAY -> isToday && !isCompleted
                        TaskFilter.WEEK -> isThisWeek && !isCompleted
                        TaskFilter.COMPLETED -> isCompleted
                    }

                    // Search logic (simple title search for now)
                    val matchesSearch = searchTerm.isBlank() || schedule.title.contains(
                        searchTerm,
                        ignoreCase = true
                    )

                    if (matchesFilter && matchesSearch) {
                        allTasks.add(
                            TaskItemUiModel.OrdinaryTask(
                                id = "schedule_${schedule.id}_${slot.id}",
                                title = schedule.title,
                                timeDescription = formatTimeDescription(
                                    startTime,
                                    endTime,
                                    schedule.isAllDay
                                ),
                                priorityTag = createPriorityTag(schedule),
                                isCompleted = isCompleted,
                                originalId = schedule.id,
                                location = schedule.location,
                                status = schedule.status,
                                startTime = startTimeLocal,
                                endTime = endTime.toLocalDateTime(TimeZone.currentSystemDefault()),
                            )
                        )
                    }
                }
            }

            val tableTimeConfigMap = tableTimeConfig?.nodes?.associate {
                it.node to (it.startTime to it.endTime)
            } ?: emptyMap()

            // --- Process Courses ---
            if (filter != TaskFilter.COMPLETED) { // Courses are never 'completed'
                courses.forEach { course ->
                    course.nodes.forEach { node ->
                        // Check if course node is in the current week for ALL, TODAY, WEEK filters
                        if (node.isInWeek(actualCurrentWeek)) {
                            val nodeDay = node.day // Monday is 1
                            val courseDate = weekDates.getOrNull(nodeDay - 1)

                            if (courseDate != null) {
                                val isToday = courseDate == today
                                val isThisWeek = true // Already filtered by isInWeek

                                val matchesFilter = when (filter) {
                                    TaskFilter.ALL -> true
                                    TaskFilter.TODAY -> isToday
                                    TaskFilter.WEEK -> isThisWeek
                                    TaskFilter.COMPLETED -> false // Should not happen here
                                }

                                val matchesSearch =
                                    searchTerm.isBlank() || course.courseName.contains(
                                        searchTerm,
                                        ignoreCase = true
                                    )

                                if (matchesFilter && matchesSearch) {
                                    allTasks.add(
                                        TaskItemUiModel.CourseTask(
                                            id = "course_${course.id}_${node.day}_${node.startNode}", // Unique ID for course instance
                                            title = course.courseName,
                                            timeDescription = formatCourseTimeDesc(node),
                                            courseColor = course.color,
                                            originalId = course.id,
                                            tableId = defaultTableId, // Pass table ID
                                            startTime = tableTimeConfigMap[node.startNode]?.first?.let {
                                                LocalDateTime(
                                                    courseDate,
                                                    it
                                                )
                                            }
                                                ?: Instant.DISTANT_PAST.toLocalDateTime(TimeZone.currentSystemDefault()),
                                            endTime = tableTimeConfigMap[node.startNode]?.second?.let {
                                                LocalDateTime(
                                                    courseDate,
                                                    it
                                                )
                                            }
                                                ?: Instant.DISTANT_PAST.toLocalDateTime(TimeZone.currentSystemDefault()),
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TODO: Implement proper sorting based on design (e.g., by time, then priority)
            val sortedTasks = allTasks.sortedBy { task ->
                when (task) {
                    is TaskItemUiModel.OrdinaryTask -> task.status != ScheduleStatus.DONE // Incomplete tasks first
                    is TaskItemUiModel.CourseTask -> false // Courses are always considered 'incomplete'
                }
            } // Basic sort for now

            TaskUiState(
                isLoading = false,
                tasks = sortedTasks,
                selectedFilter = filter,
                searchTerm = searchTerm
            )
        }
    }

    // --- Helper Functions ---

    private fun formatTimeDescription(
        startTime: Instant,
        endTime: Instant,
        isAllDay: Boolean
    ): String {
        val tz = TimeZone.currentSystemDefault()
        val startDateTime = startTime.toLocalDateTime(tz)
        val endDateTime = endTime.toLocalDateTime(tz)
        val today = Clock.System.now().toLocalDateTime(tz).date
        val tomorrow = today.plus(1, DateTimeUnit.DAY)

        val datePrefix = when (startDateTime.date) {
            today -> "今天"
            tomorrow -> "明天"
            else -> {
                // Check if it's within the current week
                val weekDates =
                    CalendarUtils.getWeekDates(CalendarUtils.getCurrentWeek()) // Needs table start date again?
                if (startDateTime.date in weekDates.first()..weekDates.last()) {
                    when (startDateTime.dayOfWeek) {
                        DayOfWeek.MONDAY -> "周一"
                        DayOfWeek.TUESDAY -> "周二"
                        DayOfWeek.WEDNESDAY -> "周三"
                        DayOfWeek.THURSDAY -> "周四"
                        DayOfWeek.FRIDAY -> "周五"
                        DayOfWeek.SATURDAY -> "周六"
                        DayOfWeek.SUNDAY -> "周日"
                        else -> startDateTime.date.toJavaLocalDate()
                            .format(DateTimeFormatter.ofPattern("MM/dd")) // Fallback
                    }
                } else {
                    startDateTime.date.toJavaLocalDate()
                        .format(DateTimeFormatter.ofPattern("MM/dd"))
                }
            }
        }

        return if (isAllDay) {
            "截止日期：$datePrefix"
        } else {
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            "截止时间：$datePrefix ${startDateTime.toJavaLocalDateTime().format(timeFormatter)}"
            // Consider showing end time or duration if relevant
            // "$datePrefix ${startDateTime.toJavaLocalDateTime().format(timeFormatter)} - ${endDateTime.toJavaLocalDateTime().format(timeFormatter)}"
        }
    }

    private fun formatCourseTimeDesc(node: com.example.todoschedule.domain.model.CourseNode): String {
        val day = when (node.day) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> "未知"
        }
        val locationString = node.room?.let { "@ $it" } ?: ""
        val endNode = node.startNode + node.step - 1
        return "$day 第 ${node.startNode}-${endNode} 节 $locationString"
    }

    private fun createPriorityTag(schedule: OrdinarySchedule): PriorityTag? {
        // Simplified logic based on design example
        // TODO: Replace with actual priority logic if available in model
        return when (schedule.title) { // Example mapping
            "完成高数作业" -> PriorityTag("紧急", Color(0xFFFEE2E2), Color(0xFFDC2626)) // Red
            "英语单词复习" -> PriorityTag("重要", Color(0xFFDCFCE7), Color(0xFF16A34A)) // Green
            "准备物理实验报告" -> PriorityTag(
                "进行中",
                Color(0xFFFEF3C7),
                Color(0xFFD97706)
            ) // Yellow
            "程序设计大作业" -> PriorityTag(
                "待开始",
                Color(0xFFF3E8FF),
                Color(0xFF9333EA)
            ) // Purple
            else -> if (schedule.status == ScheduleStatus.DONE) PriorityTag(
                "已完成",
                Color(0xFFF3F4F6),
                Color(0xFF4B5563)
            ) else null
        }
    }

    // --- Public Functions for UI Interaction ---

    fun updateFilter(newFilter: TaskFilter) {
        _uiState.update { it.copy(selectedFilter = newFilter) }
        // Data reloading is handled by the combine logic in init
    }

    fun updateSearchTerm(newSearchTerm: String) {
        _uiState.update { it.copy(searchTerm = newSearchTerm) }
        // Data reloading is handled by the combine logic in init
    }

    fun toggleTaskComplete(taskItem: TaskItemUiModel.OrdinaryTask) {
        viewModelScope.launch {
            try {
                // Fetch the full schedule to update
                val scheduleFlow =
                    getOrdinarySchedulesUseCase(currentUserIdFlow.value?.toInt() ?: -1)
                        .map { list -> list.find { it.id == taskItem.originalId } }
                        .firstOrNull()

                scheduleFlow?.let { scheduleToUpdate ->
                    val newStatus =
                        if (taskItem.isCompleted) ScheduleStatus.TODO else ScheduleStatus.DONE
                    val updatedSchedule = scheduleToUpdate.copy(status = newStatus)
                    updateOrdinaryScheduleUseCase(updatedSchedule)
                    // State will update automatically via the flow observation
                } ?: run {
                    Log.e(
                        "TaskViewModel",
                        "Schedule not found for toggling complete: ${taskItem.originalId}"
                    )
                    _uiState.update { it.copy(errorMessage = "更新任务状态失败") }
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error toggling task complete", e)
                _uiState.update { it.copy(errorMessage = "更新任务状态失败: ${e.message}") }
            }
        }
    }

}
 