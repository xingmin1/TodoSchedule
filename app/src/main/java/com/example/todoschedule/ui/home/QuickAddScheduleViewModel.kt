package com.example.todoschedule.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.data.database.converter.ScheduleStatus
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.model.TimeSlot
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.use_case.course.AddCourseUseCase
import com.example.todoschedule.domain.use_case.ordinary_schedule.AddOrdinaryScheduleUseCase
import com.example.todoschedule.ui.theme.ColorSchemeEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * 日程类型
 */
enum class ScheduleCategory {
    COURSE, // 课程
    HOMEWORK, // 作业
    EXAM, // 考试
    ONLINE_CLASS, // 网课
    REVIEW, // 复习
    ORDiNARY // 普通日程
}

/**
 * 快速添加日程/课程的 UI 状态
 */
data class QuickAddScheduleUiState(
    val title: String = "", // 标题
    val selectedCategory: ScheduleCategory = ScheduleCategory.COURSE, // 选中的类别
    val showDatePicker: Boolean = false, // 是否显示日期选择器
    val showStartTimePicker: Boolean = false, // 是否显示时间选择器
    val showEndTimePicker: Boolean = false, // 是否显示结束时间选择器
    val selectedDate: LocalDate = Clock.System.now() // 默认为当前日期
        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val startTime: LocalTime? = null, // 开始时间
    val endTime: LocalTime? = null, // 结束时间
    val location: String = "", // 地点
    val teacher: String = "", // 教师
    val credit: Float = 0f, // 学分
    val weekRange: String = "", // 周次范围
    val isNotify: Boolean = false, // 是否提醒
    val isRepeat: Boolean = false, // 是否重复
    val errorMessage: String? = null, // 错误信息
    val selectedColor: ColorSchemeEnum = ColorSchemeEnum.PRIMARY, // 选中的颜色，默认主题色
    val notifyTime: Int = 0, // 提前提醒时间（分钟），0为准时
    val repeatRule: String = "", // 重复规则，如"每天"、"每周"等
    val startNode: Int? = null, // 课程开始节次
    val step: Int? = null, // 节数
    val detail: String = "" // 详情，仅普通日程用
)

/**
 * 快速添加日程/课程的 ViewModel
 */
@HiltViewModel
class QuickAddScheduleViewModel @Inject constructor(
    private val addOrdinaryScheduleUseCase: AddOrdinaryScheduleUseCase,
    private val addCourseUseCase: AddCourseUseCase,
    private val sessionRepository: SessionRepository,
    private val globalSettingRepository: GlobalSettingRepository
) : ViewModel() {

    // UI 状态
    private val _uiState = MutableStateFlow(QuickAddScheduleUiState())
    val uiState: StateFlow<QuickAddScheduleUiState> = _uiState.asStateFlow()

    /**
     * 更新标题
     */
    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    /**
     * 更新类别
     */
    fun onCategoryChange(category: ScheduleCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    /**
     * 显示/隐藏日期选择器
     */
    fun showDatePicker(show: Boolean) {
        _uiState.update { it.copy(showDatePicker = show) }
    }

    /**
     * 显示/隐藏时间选择器
     */
    fun showStartTimePicker(show: Boolean) {
        _uiState.update { it.copy(showStartTimePicker = show) }
    }

    /**
     * 显示/隐藏结束时间选择器
     */
    fun showEndTimePicker(show: Boolean) {
        _uiState.update { it.copy(showEndTimePicker = show) }
    }

    /**
     * 更新选择的日期
     */
    fun onDateSelected(date: LocalDate) {
        _uiState.update {
            it.copy(
                selectedDate = date,
                showDatePicker = false
            )
        }
    }

    /**
     * 更新开始时间
     */
    fun onTimeSelected(time: LocalTime) {
        _uiState.update {
            it.copy(
                startTime = time,
                showStartTimePicker = false
            )
        }
    }

    /**
     * 更新结束时间
     */
    fun onEndTimeSelected(time: LocalTime) {
        _uiState.update { it.copy(endTime = time) }
    }

    /**
     * 更新地点
     */
    fun onLocationChange(location: String) {
        _uiState.update { it.copy(location = location) }
    }

    /**
     * 更新教师
     */
    fun onTeacherChange(teacher: String) {
        _uiState.update { it.copy(teacher = teacher) }
    }

    /**
     * 更新学分
     */
    fun onCreditChange(credit: Float) {
        _uiState.update { it.copy(credit = credit) }
    }

    /**
     * 更新周次范围
     */
    fun onWeekRangeChange(weekRange: String) {
        _uiState.update { it.copy(weekRange = weekRange) }
    }

    /**
     * 更新是否提醒
     */
    fun onNotifyChange(notify: Boolean) {
        _uiState.update { it.copy(isNotify = notify) }
    }

    /**
     * 更新是否重复
     */
    fun onRepeatChange(repeat: Boolean) {
        _uiState.update { it.copy(isRepeat = repeat) }
    }

    /**
     * 清除错误消息
     */
    fun consumeErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 选择颜色
     */
    fun onColorChange(color: ColorSchemeEnum) {
        _uiState.update { it.copy(selectedColor = color) }
    }

    /**
     * 设置提醒时间（分钟）
     */
    fun onNotifyTimeChange(minute: Int) {
        _uiState.update { it.copy(notifyTime = minute) }
    }

    /**
     * 设置重复规则
     */
    fun onRepeatRuleChange(rule: String) {
        _uiState.update { it.copy(repeatRule = rule) }
    }

    /**
     * 设置课程开始节次
     */
    fun onStartNodeChange(node: Int) {
        _uiState.update { it.copy(startNode = node) }
    }

    /**
     * 设置课程节数
     */
    fun onStepChange(step: Int) {
        _uiState.update { it.copy(step = step) }
    }

    /**
     * 更新普通日程详情
     */
    fun onDetailChange(detail: String) {
        _uiState.update { it.copy(detail = detail) }
    }

    /**
     * 保存日程/课程
     */
    fun saveSchedule() {
        viewModelScope.launch {
            try {
                // 验证必填字段
                if (_uiState.value.title.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "请输入标题") }
                    return@launch
                }

                if (_uiState.value.startTime == null) {
                    _uiState.update { it.copy(errorMessage = "请选择时间") }
                    return@launch
                }

                // 获取当前用户ID
                val userId = sessionRepository.currentUserIdFlow.first()?.toInt()
                    ?: throw IllegalStateException("用户未登录")

                // 获取当前课表ID
                val defaultTableIds = globalSettingRepository.getDefaultTableIds(userId).first()
                val defaultTableId = defaultTableIds.firstOrNull()
                    ?: throw IllegalStateException("未设置默认课表")

                when (_uiState.value.selectedCategory) {
                    ScheduleCategory.COURSE -> {
                        // 验证课程特有字段
                        if (_uiState.value.weekRange.isBlank()) {
                            _uiState.update { it.copy(errorMessage = "请输入周次范围") }
                            return@launch
                        }

                        // 解析周次范围
                        val weekRange = _uiState.value.weekRange.split("-")
                        if (weekRange.size != 2) {
                            _uiState.update { it.copy(errorMessage = "周次范围格式错误，请使用1-16这样的格式") }
                            return@launch
                        }

                        val startWeek = weekRange[0].toIntOrNull()
                        val endWeek = weekRange[1].toIntOrNull()
                        if (startWeek == null || endWeek == null || startWeek > endWeek) {
                            _uiState.update { it.copy(errorMessage = "周次范围格式错误") }
                            return@launch
                        }

                        // 创建课程
                        val startNode = _uiState.value.startNode ?: 1
                        val step = _uiState.value.step ?: 2
                        val course = Course(
                            courseName = _uiState.value.title,
                            color = _uiState.value.selectedColor,
                            room = _uiState.value.location.takeIf { it.isNotEmpty() },
                            teacher = _uiState.value.teacher.takeIf { it.isNotEmpty() },
                            credit = _uiState.value.credit.takeIf { it > 0f },
                            nodes = listOf(
                                CourseNode(
                                    day = _uiState.value.selectedDate.dayOfWeek.isoDayNumber,
                                    startNode = startNode,
                                    step = step,
                                    startWeek = startWeek,
                                    endWeek = endWeek,
                                    weekType = 0, // 默认每周
                                    room = _uiState.value.location.takeIf { it.isNotEmpty() },
                                    teacher = _uiState.value.teacher.takeIf { it.isNotEmpty() }
                                )
                            )
                        )

                        addCourseUseCase(course, defaultTableId)
                    }

                    else -> {
                        // 创建普通日程
                        val schedule = OrdinarySchedule(
                            userId = userId,
                            title = _uiState.value.title,
                            description = _uiState.value.detail,
                            location = _uiState.value.location,
                            color = _uiState.value.selectedColor,
                            isAllDay = false,
                            status = ScheduleStatus.TODO,
                            timeSlots = listOf(
                                TimeSlot(
                                    startTime = _uiState.value.selectedDate
                                        .atTime(_uiState.value.startTime!!)
                                        .toInstant(TimeZone.currentSystemDefault())
                                        .toEpochMilliseconds(),
                                    endTime = _uiState.value.selectedDate
                                        .atTime(
                                            _uiState.value.endTime ?: LocalTime(
                                                hour = (_uiState.value.startTime!!.hour) % 24,
                                                minute = _uiState.value.startTime!!.minute
                                            )
                                        )
                                        .toInstant(TimeZone.currentSystemDefault())
                                        .toEpochMilliseconds(),
                                    scheduleType = when (_uiState.value.selectedCategory) {
                                        ScheduleCategory.HOMEWORK -> ScheduleType.HOMEWORK
                                        ScheduleCategory.EXAM -> ScheduleType.EXAM
                                        ScheduleCategory.ONLINE_CLASS -> ScheduleType.ONLINE_COURSE
                                        ScheduleCategory.REVIEW -> ScheduleType.REVIEW
                                        else -> ScheduleType.ORDINARY
                                    },
                                    scheduleId = 0,
                                    userId = userId
                                )
                            )
                        )

                        addOrdinaryScheduleUseCase(schedule)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "保存失败") }
            }
        }
    }
}
