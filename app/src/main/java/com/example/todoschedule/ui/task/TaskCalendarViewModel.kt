package com.example.todoschedule.ui.task

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.core.utils.PermissionManager
import com.example.todoschedule.data.database.converter.ScheduleStatus
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.domain.use_case.ordinary_schedule.GetOrdinarySchedulesUseCase
import com.example.todoschedule.domain.use_case.table_time_config.GetDefaultTableTimeConfigUseCase
import com.example.todoschedule.domain.utils.CalendarSyncManager
import com.example.todoschedule.domain.utils.CalendarUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * 表示UI层的任务模型
 */
data class TaskUiModel(
    val id: Long,
    val title: String,
    val description: String?,
    val dueDate: Calendar,
    val formattedDueDate: String,
    val isCompleted: Boolean
)

/**
 * 任务日历同步界面的状态
 */
data class TaskCalendarSyncState(
    val tasks: List<TaskItemUiModel> = emptyList(),
    val selectedTaskIds: Set<String> = emptySet(),
    val selectedCalendarId: Long? = null,
    val isSyncing: Boolean = false,
    val syncResultMessage: String? = null
)

/**
 * 任务日历同步ViewModel
 * 管理任务与设备日历同步的状态和操作
 */
@HiltViewModel
class TaskCalendarViewModel @Inject constructor(
    private val calendarSyncManager: CalendarSyncManager,
    private val sessionRepository: SessionRepository,
    private val getOrdinarySchedulesUseCase: GetOrdinarySchedulesUseCase,
    private val courseRepository: CourseRepository,
    private val globalSettingRepository: GlobalSettingRepository,
    private val tableRepository: TableRepository,
    private val permissionManager: PermissionManager,
    private val getDefaultTableTimeConfigUseCase: GetDefaultTableTimeConfigUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskCalendarSyncState())
    val uiState: StateFlow<TaskCalendarSyncState> = _uiState.asStateFlow()

    private val _availableCalendars = MutableStateFlow<List<Pair<Long, String>>>(emptyList())
    val availableCalendars: StateFlow<List<Pair<Long, String>>> = _availableCalendars.asStateFlow()

    // 日历权限状态
    private val _hasCalendarPermissions = MutableStateFlow(false)
    val hasCalendarPermissions: StateFlow<Boolean> = _hasCalendarPermissions.asStateFlow()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * 检查是否拥有日历权限
     */
    fun checkCalendarPermissions() {
        _hasCalendarPermissions.value = permissionManager.hasCalendarPermissions()
    }

    /**
     * 获取日历权限声明数组
     */
    fun getCalendarPermissions(): Array<String> {
        return PermissionManager.CALENDAR_PERMISSIONS
    }

    /**
     * 权限请求结果处理
     */
    fun onPermissionsResult(granted: Boolean) {
        _hasCalendarPermissions.value = granted
    }

    /**
     * 自动选择系统默认日历
     * 简化用户体验，自动获取系统中第一个可用的日历
     */
    fun autoSelectCalendar() {
        // 如果没有权限，不执行操作
        if (!_hasCalendarPermissions.value) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val calendars = calendarSyncManager.getAvailableCalendars()
                // 选择第一个可用的日历
                if (calendars.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            selectedCalendarId = calendars.keys.first()
                        )
                    }
                }
            } catch (e: Exception) {
                // 日历访问失败，不影响UI
            }
        }
    }

    /**
     * 选择所有任务
     * 在非自定义模式下自动选择所有任务
     */
    fun selectAllTasks() {
        val allTaskIds = _uiState.value.tasks.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedTaskIds = allTaskIds)
    }

    /**
     * 取消选择所有任务
     */
    fun deselectAllTasks() {
        _uiState.value = _uiState.value.copy(selectedTaskIds = emptySet())
    }

    /**
     * 获取任务数据
     */
    private suspend fun getAllTasks(): List<TaskItemUiModel> {
        val userId = sessionRepository.currentUserIdFlow.value?.toInt() ?: return emptyList()

        // 获取默认课表ID
        val defaultTableIds = globalSettingRepository.getDefaultTableIds(userId).first()
        if (defaultTableIds.isEmpty()) return emptyList()

        val defaultTableId = defaultTableIds.first()

        // 获取当前日期
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        // 获取当前周信息
        val table = tableRepository.getTableById(defaultTableId).firstOrNull()
        val tableStartDate = table?.startDate ?: CalendarUtils.getCurrentWeekStart()
        val currentWeek = CalendarUtils.getCurrentWeek(tableStartDate)
        val weekDates = CalendarUtils.getWeekDates(currentWeek, tableStartDate)

        Log.d(
            "TaskCalendarVM",
            "当前日期: $today, 当前周: $currentWeek, 周开始: ${weekDates.first()}, 周结束: ${weekDates.last()}"
        )

        // 组合普通日程和课程数据
        return combine(
            getOrdinarySchedulesUseCase(userId),
            courseRepository.getCoursesByTableId(defaultTableId)
        ) { schedules, courses ->
            val tasksList = mutableListOf<TaskItemUiModel>()

            // 处理普通日程
            schedules.forEach { schedule ->
                schedule.timeSlots.forEach { slot ->
                    val startInstant = Instant.fromEpochMilliseconds(slot.startTime)
                    val endInstant = Instant.fromEpochMilliseconds(slot.endTime)
                    val tz = TimeZone.currentSystemDefault()
                    val startTime = startInstant.toLocalDateTime(tz)
                    val endTime = endInstant.toLocalDateTime(tz)

                    // 添加调试日志
                    Log.d(
                        "TaskCalendarVM",
                        "普通日程: ${schedule.title}, 日期: ${startTime.date}, 与今天比较: ${startTime.date == today}"
                    )

                    tasksList.add(
                        TaskItemUiModel.OrdinaryTask(
                            id = "schedule_${schedule.id}_${slot.id}",
                            title = schedule.title,
                            timeDescription = "${startTime.date} ${startTime.hour}:${startTime.minute}",
                            priorityTag = null,
                            isCompleted = schedule.status == ScheduleStatus.DONE,
                            originalId = schedule.id,
                            startTime = startTime,
                            endTime = endTime,
                            status = schedule.status,
                            location = schedule.location
                        )
                    )
                }
            }

            // 处理课程 - 使用正确的时间计算
            val timeConfigFlow = getDefaultTableTimeConfigUseCase(defaultTableId)
            val timeConfig = timeConfigFlow.firstOrNull()

            courses.forEach { course ->
                course.nodes.forEach { node ->
                    // 判断节点是否在当前周
                    if (node.isInWeek(currentWeek)) {
                        // 计算课程日期 - 使用当前周对应的星期几日期
                        val nodeDay = node.day // 1-7表示周一到周日
                        val courseDate = weekDates.getOrNull(nodeDay - 1) // 获取对应星期几的日期

                        if (courseDate != null) {
                            // 获取节点的开始和结束时间
                            val startNodeTime =
                                timeConfig?.nodes?.find { it.node == node.startNode }?.startTime
                            val endNodeTime =
                                timeConfig?.nodes?.find { it.node == node.startNode + node.step - 1 }?.endTime

                            // 使用指定日期和时间创建LocalDateTime
                            val startTime = startNodeTime?.let {
                                LocalDateTime(courseDate, it)
                            } ?: LocalDateTime(courseDate, LocalTime(8 + node.startNode, 0))

                            val endTime = endNodeTime?.let {
                                LocalDateTime(courseDate, it)
                            } ?: LocalDateTime(
                                courseDate,
                                LocalTime(8 + node.startNode + node.step, 0)
                            )

                            // 添加调试日志
                            Log.d(
                                "TaskCalendarVM",
                                "课程: ${course.courseName}, 日期: $courseDate, 与今天比较: ${courseDate == today}"
                            )

                            // 使用UUID生成绝对唯一的ID
                            val uniqueId = "course_" + UUID.randomUUID().toString()

                            tasksList.add(
                                TaskItemUiModel.CourseTask(
                                    id = uniqueId,
                                    title = course.courseName,
                                    timeDescription = formatCourseTimeDesc(node),
                                    courseColor = course.color,
                                    originalId = course.id,
                                    tableId = defaultTableId,
                                    startTime = startTime,
                                    endTime = endTime
                                )
                            )
                        }
                    }
                }
            }

            tasksList
        }.first()
    }

    /**
     * 格式化课程时间描述
     */
    private fun formatCourseTimeDesc(node: CourseNode): String {
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

    /**
     * 加载符合过滤条件的任务
     */
    fun loadTasks(filter: String) {
        // 如果没有权限，不加载任务
        if (!_hasCalendarPermissions.value) return

        viewModelScope.launch {
            val allTasks = getAllTasks()

            // 根据筛选条件过滤任务
            val filteredTasks = when (filter) {
                "today" -> {
                    val todayTasks = filterTodayTasks(allTasks)
                    // 调试信息
                    Log.d("TaskCalendarVM", "今日任务: ${todayTasks.size}/${allTasks.size}")
                    todayTasks
                }

                "week" -> {
                    val weekTasks = filterWeekTasks(allTasks)
                    // 调试信息
                    Log.d("TaskCalendarVM", "本周任务: ${weekTasks.size}/${allTasks.size}")
                    weekTasks
                }

                "custom", "all" -> allTasks
                else -> allTasks
            }

            // 更新UI状态
            _uiState.value = _uiState.value.copy(
                tasks = filteredTasks,
                // 清除之前的选择
                selectedTaskIds = emptySet()
            )

            // 如果不是自定义模式，自动全选任务
            if (filter != "custom") {
                selectAllTasks()
            }
        }
    }

    /**
     * 过滤今天的任务
     */
    private fun filterTodayTasks(tasks: List<TaskItemUiModel>): List<TaskItemUiModel> {
        // 获取今天的日期
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        Log.d("TaskCalendarVM", "当前日期: $today")

        // 直接比较日期，与TaskViewModel中的逻辑一致
        return tasks.filter { task ->
            val taskDate = task.startTime.date

            // 使用直接比较 == 而不是比较年月日各个部分
            val isToday = taskDate == today

            Log.d(
                "TaskCalendarVM",
                "任务 '${task.title}' 日期: $taskDate, 是否为今日任务: $isToday"
            )

            isToday
        }
    }

    /**
     * 过滤本周的任务
     */
    private fun filterWeekTasks(tasks: List<TaskItemUiModel>): List<TaskItemUiModel> {
        // 使用与TaskViewModel相同的方法获取周开始和结束
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        // 获取本周的日期范围 (周一到周日)
        val currentWeek = com.example.todoschedule.domain.utils.CalendarUtils.getCurrentWeek()
        val weekDates =
            com.example.todoschedule.domain.utils.CalendarUtils.getWeekDates(currentWeek)
        val weekStart = weekDates.first()
        val weekEnd = weekDates.last()

        Log.d("TaskCalendarVM", "本周一: $weekStart, 本周日: $weekEnd, 今天: $today")

        return tasks.filter { task ->
            val taskDate = task.startTime.date

            // 检查日期是否在本周范围内，与TaskViewModel中的逻辑一致
            val isInWeek = taskDate in weekStart..weekEnd

            Log.d("TaskCalendarVM", "任务 '${task.title}' 日期: $taskDate, 是否在本周: $isInWeek")

            isInWeek
        }
    }

    /**
     * 过滤即将到来的任务
     */
    private fun filterUpcomingTasks(tasks: List<TaskItemUiModel>): List<TaskItemUiModel> {
        val now = Calendar.getInstance()
        return tasks.filter { task ->
            val taskStartTime = Calendar.getInstance().apply {
                set(
                    task.startTime.year,
                    task.startTime.monthNumber - 1,
                    task.startTime.dayOfMonth,
                    task.startTime.hour,
                    task.startTime.minute
                )
            }
            !task.isCompleted && taskStartTime.after(now)
        }
    }

    /**
     * 过滤已完成的任务
     */
    private fun filterCompletedTasks(tasks: List<TaskItemUiModel>): List<TaskItemUiModel> {
        return tasks.filter { it.isCompleted }
    }

    /**
     * 加载设备上可用的日历
     */
    fun loadAvailableCalendars() {
        // 如果没有权限，不加载日历
        if (!_hasCalendarPermissions.value) return

        viewModelScope.launch(Dispatchers.IO) {
            val calendars = calendarSyncManager.getAvailableCalendars()
            withContext(Dispatchers.Main) {
                _availableCalendars.value = calendars.entries.map { (id, name) ->
                    Pair(id, name)
                }.sortedBy { it.second }
            }
        }
    }

    /**
     * 选择目标日历
     */
    fun selectCalendar(calendarId: Long) {
        _uiState.value = _uiState.value.copy(selectedCalendarId = calendarId)
    }

    /**
     * 切换任务选择状态
     */
    fun toggleTaskSelection(taskId: String) {
        val currentSelection = _uiState.value.selectedTaskIds
        val newSelection = if (currentSelection.contains(taskId)) {
            currentSelection - taskId
        } else {
            currentSelection + taskId
        }

        _uiState.value = _uiState.value.copy(selectedTaskIds = newSelection)
    }

    /**
     * 将选中的任务同步到设备日历
     */
    fun syncSelectedTasksToCalendar() {
        // 如果没有权限，不执行同步
        if (!_hasCalendarPermissions.value) return

        val calendarId = _uiState.value.selectedCalendarId ?: return
        val selectedIds = _uiState.value.selectedTaskIds
        if (selectedIds.isEmpty()) return

        _uiState.value = _uiState.value.copy(isSyncing = true, syncResultMessage = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 获取选中的任务
                val tasksToSync =
                    _uiState.value.tasks.filter { task -> selectedIds.contains(task.id) }

                // 执行同步
                val successCount = calendarSyncManager.batchSyncTasksToCalendar(
                    tasksToSync,
                    calendarId
                )

                // 更新UI状态
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncResultMessage = "成功同步 $successCount/${selectedIds.size} 个任务到日历"
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncResultMessage = "同步失败: ${e.message}"
                    )
                }
            }
        }
    }
} 