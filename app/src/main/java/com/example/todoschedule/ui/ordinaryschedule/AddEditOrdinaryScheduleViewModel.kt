package com.example.todoschedule.ui.ordinaryschedule

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.data.database.converter.ScheduleStatus
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.model.TimeSlot
import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.use_case.ordinary_schedule.AddOrdinaryScheduleUseCase
import com.example.todoschedule.domain.use_case.ordinary_schedule.GetOrdinaryScheduleByIdUseCase
import com.example.todoschedule.domain.use_case.ordinary_schedule.UpdateOrdinaryScheduleUseCase
import com.example.todoschedule.ui.navigation.AppRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toKotlinTimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.ZoneId
import javax.inject.Inject

// 状态数据类
data class AddEditOrdinaryScheduleUiState(
    val scheduleId: Int? = null, // Track the ID for editing
    val title: String = "",
    val description: String? = null,
    val location: String? = null,
    val category: String? = null,
    val color: String? = null,
    val status: ScheduleStatus = ScheduleStatus.TODO,
    val startDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endDate: LocalDate? = null,
    val endTime: LocalTime? = null,
    val showStartDatePicker: Boolean = false,
    val showStartTimePicker: Boolean = false,
    val showEndDatePicker: Boolean = false,
    val showEndTimePicker: Boolean = false,
    val isLoading: Boolean = false,
    val isInitializing: Boolean = true, // Indicate initial loading for edit mode
    val errorMessage: String? = null,
    val isSaved: Boolean = false // 标记是否保存成功以触发导航返回
) {
    val isEditMode: Boolean get() = scheduleId != null
}

@HiltViewModel
class AddEditOrdinaryScheduleViewModel @Inject constructor(
    private val addOrdinaryScheduleUseCase: AddOrdinaryScheduleUseCase,
    private val getOrdinaryScheduleByIdUseCase: GetOrdinaryScheduleByIdUseCase,
    private val updateOrdinaryScheduleUseCase: UpdateOrdinaryScheduleUseCase,
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditOrdinaryScheduleUiState())
    val uiState: StateFlow<AddEditOrdinaryScheduleUiState> = _uiState.asStateFlow()

    // Get potential scheduleId from navigation arguments
    private val scheduleIdFromNav: Int? =
        savedStateHandle.get<Int>(AppRoutes.AddEditOrdinarySchedule.ARG_SCHEDULE_ID)
            ?.let { if (it == -1) null else it } // Convert default value -1 to null

    init {
        if (scheduleIdFromNav != null) {
            loadScheduleForEditing(scheduleIdFromNav)
        } else {
            // It's 'add' mode, finish initializing
            _uiState.update { it.copy(isInitializing = false) }
        }
    }

    private fun loadScheduleForEditing(scheduleId: Int) {
        _uiState.update {
            it.copy(
                isLoading = true,
                isInitializing = true,
                scheduleId = scheduleId
            )
        }
        viewModelScope.launch {
            getOrdinaryScheduleByIdUseCase(scheduleId).firstOrNull()?.let { schedule ->
                // Assuming one TimeSlot for simplicity in this initial edit implementation
                // TODO: Handle multiple or no time slots properly
                val timeSlot = schedule.timeSlots.firstOrNull()
                val startDateTime = timeSlot?.let {
                    Instant.fromEpochMilliseconds(it.startTime)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                }
                val endDateTime = timeSlot?.let {
                    Instant.fromEpochMilliseconds(it.endTime)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isInitializing = false,
                        title = schedule.title,
                        description = schedule.description,
                        location = schedule.location,
                        category = schedule.category,
                        color = schedule.color,
                        status = schedule.status ?: ScheduleStatus.TODO,
                        startDate = startDateTime?.date,
                        startTime = startDateTime?.time,
                        endDate = endDateTime?.date,
                        endTime = endDateTime?.time
                    )
                }
            } ?: run {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isInitializing = false,
                        errorMessage = "无法加载要编辑的日程"
                    )
                }
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onDescriptionChange(newDescription: String) {
        _uiState.update { it.copy(description = newDescription) }
    }

    fun onLocationChange(newLocation: String) {
        _uiState.update { it.copy(location = newLocation) }
    }

    fun onCategoryChange(newCategory: String) {
        _uiState.update { it.copy(category = newCategory) }
    }

    fun onColorChange(newColor: String) {
        _uiState.update { it.copy(color = newColor) }
    }

    fun onStatusChange(newStatus: ScheduleStatus) {
        _uiState.update { it.copy(status = newStatus) }
    }

    fun onStartDateSelected(date: LocalDate?) {
        _uiState.update { it.copy(startDate = date, showStartDatePicker = false) }
    }

    fun onStartTimeSelected(time: LocalTime?) {
        _uiState.update { it.copy(startTime = time, showStartTimePicker = false) }
    }

    fun onEndDateSelected(date: LocalDate?) {
        _uiState.update { it.copy(endDate = date, showEndDatePicker = false) }
    }

    fun onEndTimeSelected(time: LocalTime?) {
        _uiState.update { it.copy(endTime = time, showEndTimePicker = false) }
    }

    // --- Picker Visibility Control ---

    fun showStartDatePicker() {
        _uiState.update { it.copy(showStartDatePicker = true) }
    }

    fun dismissStartDatePicker() {
        _uiState.update { it.copy(showStartDatePicker = false) }
    }

    // ... Similar functions for startTime, endDate, endTime pickers ...
    fun showStartTimePicker() {
        _uiState.update { it.copy(showStartTimePicker = true) }
    }

    fun dismissStartTimePicker() {
        _uiState.update { it.copy(showStartTimePicker = false) }
    }

    fun showEndDatePicker() {
        _uiState.update { it.copy(showEndDatePicker = true) }
    }

    fun dismissEndDatePicker() {
        _uiState.update { it.copy(showEndDatePicker = false) }
    }

    fun showEndTimePicker() {
        _uiState.update { it.copy(showEndTimePicker = true) }
    }

    fun dismissEndTimePicker() {
        _uiState.update { it.copy(showEndTimePicker = false) }
    }

    fun saveSchedule() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.title.isBlank()) {
                _uiState.update { it.copy(errorMessage = "标题不能为空") }
                return@launch
            }
            if (currentState.startDate == null || currentState.startTime == null ||
                currentState.endDate == null || currentState.endTime == null
            ) {
                _uiState.update { it.copy(errorMessage = "请选择完整的开始和结束时间") }
                return@launch
            }

            // Validate end time is after start time
            val startDateTime = currentState.startDate.atTime(currentState.startTime)
            val endDateTime = currentState.endDate.atTime(currentState.endTime)
            if (endDateTime <= startDateTime) {
                _uiState.update { it.copy(errorMessage = "结束时间必须晚于开始时间") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val currentUserId = sessionRepository.currentUserIdFlow.first()
                if (currentUserId == null || currentUserId == -1L) {
                    Log.e("AddEditVM", "Save failed: Current user not logged in or invalid ID.")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "无法获取用户信息，请重新登录"
                        )
                    }
                    return@launch
                }
                val userIdInt = currentUserId.toInt()
                Log.d("AddEditVM", "Current User ID for TimeSlot: $userIdInt")

                // Convert to Milliseconds
                val zoneId = ZoneId.systemDefault()
                val startMillis =
                    startDateTime.toInstant(zoneId.toKotlinTimeZone()).toEpochMilliseconds()
                val endMillis =
                    endDateTime.toInstant(zoneId.toKotlinTimeZone()).toEpochMilliseconds()

                // Create TimeSlot using the fetched currentUserId
                val timeSlot = TimeSlot(
                    startTime = startMillis,
                    endTime = endMillis,
                    scheduleType = ScheduleType.ORDINARY,
                    scheduleId = currentState.scheduleId ?: 0,
                    userId = userIdInt
                )

                val scheduleToSave = OrdinarySchedule(
                    id = currentState.scheduleId ?: 0,
                    userId = userIdInt,
                    title = currentState.title,
                    description = currentState.description,
                    location = currentState.location,
                    category = currentState.category,
                    color = currentState.color,
                    status = currentState.status,
                    timeSlots = listOf(timeSlot)
                )

                if (currentState.isEditMode) {
                    if (scheduleToSave.userId != userIdInt) {
                        Log.e(
                            "AddEditVM",
                            "Update failed: User ID mismatch. Cannot edit other user's schedule."
                        )
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "无法编辑此日程"
                            )
                        }
                        return@launch
                    }
                    updateOrdinaryScheduleUseCase(scheduleToSave)
                    Log.d("AddEditVM", "Updating schedule: ${scheduleToSave.id}")
                } else {
                    addOrdinaryScheduleUseCase(scheduleToSave)
                    Log.d("AddEditVM", "Adding new schedule")
                }

                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                Log.e("AddEditVM", "Save failed with exception", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "保存失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun consumeErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeSavedEvent() {
        _uiState.update { it.copy(isSaved = false) }
    }
} 