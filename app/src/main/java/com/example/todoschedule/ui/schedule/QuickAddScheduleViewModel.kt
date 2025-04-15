package com.example.todoschedule.ui.schedule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.data.database.converter.ScheduleStatus
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.model.TimeSlot
import com.example.todoschedule.domain.repository.SessionRepository
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
import kotlinx.datetime.toInstant
import kotlinx.datetime.toKotlinTimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.ZoneId
import javax.inject.Inject

// 状态数据类 for Quick Add
data class QuickAddScheduleUiState(
    val title: String = "",
    val description: String? = null,
    val selectedDate: LocalDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val showDatePicker: Boolean = false,
    val showStartTimePicker: Boolean = false,
    val showEndTimePicker: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSaved: Boolean = false // 标记是否保存成功
)

@HiltViewModel
class QuickAddScheduleViewModel @Inject constructor(
    private val addOrdinaryScheduleUseCase: AddOrdinaryScheduleUseCase,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickAddScheduleUiState())
    val uiState: StateFlow<QuickAddScheduleUiState> = _uiState.asStateFlow()

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onDescriptionChange(newDescription: String) {
        _uiState.update { it.copy(description = newDescription.ifBlank { null }) } // Store null if blank
    }

    fun onDateSelected(date: LocalDate?) {
        // Only update if date is not null, keep current date otherwise
        _uiState.update { it.copy(selectedDate = date ?: it.selectedDate, showDatePicker = false) }
    }

    fun onStartTimeSelected(time: LocalTime?) {
        _uiState.update { it.copy(startTime = time, showStartTimePicker = false) }
        // Automatically open end time picker if start is set and end isn't, or if end is before start
        if (time != null) {
            val currentState = _uiState.value
            if (currentState.endTime == null || (currentState.selectedDate.atTime(currentState.endTime!!) <= currentState.selectedDate.atTime(
                    time
                ))
            ) {
                showEndTimePicker(true)
            }
        }
    }

    fun onEndTimeSelected(time: LocalTime?) {
        _uiState.update { it.copy(endTime = time, showEndTimePicker = false) }
    }

    // --- Picker Visibility Control ---

    fun showDatePicker(show: Boolean) {
        _uiState.update { it.copy(showDatePicker = show) }
    }

    fun showStartTimePicker(show: Boolean) {
        _uiState.update { it.copy(showStartTimePicker = show) }
    }

    fun showEndTimePicker(show: Boolean) {
        _uiState.update { it.copy(showEndTimePicker = show) }
    }

    /**
     * 重置所有UI状态为初始值
     */
    fun resetState() {
        _uiState.update {
            QuickAddScheduleUiState(
                // 保持当前日期，其他都重置
                selectedDate = _uiState.value.selectedDate
            )
        }
    }

    fun saveSchedule(onDismiss: () -> Unit) { // Pass dismiss lambda
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.title.isBlank()) {
                _uiState.update { it.copy(errorMessage = "标题不能为空") }
                return@launch
            }
            // Start and end times are optional for quick add, but if one is set, the other should be too (or have a default)
            // Let's require both if either is set for simplicity here
            if ((currentState.startTime != null || currentState.endTime != null) &&
                (currentState.startTime == null || currentState.endTime == null)
            ) {
                _uiState.update { it.copy(errorMessage = "请同时选择开始和结束时间") }
                return@launch
            }

            // Validate end time is after start time if both are set
            if (currentState.startTime != null && currentState.endTime != null) {
                val startDateTime = currentState.selectedDate.atTime(currentState.startTime)
                val endDateTime = currentState.selectedDate.atTime(currentState.endTime)
                if (endDateTime < startDateTime) {
                    _uiState.update { it.copy(errorMessage = "结束时间必须晚于开始时间") }
                    return@launch
                }
            }


            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val currentUserId = sessionRepository.currentUserIdFlow.first()
                if (currentUserId == null || currentUserId == -1L) {
                    Log.e("QuickAddVM", "Save failed: Current user not logged in or invalid ID.")
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "无法获取用户信息，请重新登录")
                    }
                    return@launch
                }
                val userIdInt = currentUserId.toInt()
                Log.d("QuickAddVM", "Current User ID for Quick Add: $userIdInt")

                // Prepare TimeSlot only if times are provided
                val timeSlots = mutableListOf<TimeSlot>()
                if (currentState.startTime != null && currentState.endTime != null) {
                    val zoneId = ZoneId.systemDefault()
                    val startMillis = currentState.selectedDate.atTime(currentState.startTime)
                        .toInstant(zoneId.toKotlinTimeZone()).toEpochMilliseconds()
                    val endMillis = currentState.selectedDate.atTime(currentState.endTime)
                        .toInstant(zoneId.toKotlinTimeZone()).toEpochMilliseconds()

                    timeSlots.add(
                        TimeSlot(
                            startTime = startMillis,
                            endTime = endMillis,
                            scheduleType = ScheduleType.ORDINARY, // Hardcoded for quick add
                            scheduleId = 0, // Will be set by Room
                            userId = userIdInt
                            // Default values for other TimeSlot fields are used
                        )
                    )
                    Log.d("QuickAddVM", "TimeSlot created: $startMillis to $endMillis")
                } else {
                    Log.d("QuickAddVM", "No time selected, creating schedule without TimeSlot.")
                }


                val scheduleToSave = OrdinarySchedule(
                    id = 0, // New schedule
                    userId = userIdInt,
                    title = currentState.title,
                    description = currentState.description,
                    color = ColorSchemeEnum.PRIMARY, // Default color
                    location = null, // Not available in quick add
                    category = null, // Not available in quick add
                    isAllDay = timeSlots.isEmpty(), // Consider it all-day if no time specified
                    status = ScheduleStatus.TODO, // Default status
                    timeSlots = timeSlots
                )

                addOrdinaryScheduleUseCase(scheduleToSave)
                Log.d("QuickAddVM", "Adding new ordinary schedule via Quick Add")

                // 更新状态为已保存，然后重置状态
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
                resetState()

                // 通知关闭底部表单
                onDismiss()

            } catch (e: Exception) {
                Log.e("QuickAddVM", "Quick Add save failed with exception", e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "保存失败: ${e.message}")
                }
            }
        }
    }

    fun consumeErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // No need for consumeSavedEvent if we dismiss immediately
} 