package com.example.todoschedule.ui.ordinaryschedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.use_case.ordinary_schedule.DeleteOrdinaryScheduleUseCase
import com.example.todoschedule.domain.use_case.ordinary_schedule.GetOrdinaryScheduleByIdUseCase
import com.example.todoschedule.domain.use_case.ordinary_schedule.UpdateOrdinaryScheduleUseCase
import com.example.todoschedule.ui.navigation.AppRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI 状态
sealed interface OrdinaryScheduleDetailUiState {
    object Loading : OrdinaryScheduleDetailUiState
    data class Success(val schedule: OrdinarySchedule) : OrdinaryScheduleDetailUiState
    data class Error(val message: String) : OrdinaryScheduleDetailUiState
}

// 事件
sealed interface OrdinaryScheduleDetailEvent {
    object NavigateBack : OrdinaryScheduleDetailEvent
    data class ShowError(val message: String) : OrdinaryScheduleDetailEvent
}

@HiltViewModel
class OrdinaryScheduleDetailViewModel @Inject constructor(
    private val getOrdinaryScheduleByIdUseCase: GetOrdinaryScheduleByIdUseCase,
    private val deleteOrdinaryScheduleUseCase: DeleteOrdinaryScheduleUseCase,
    private val updateOrdinaryScheduleUseCase: UpdateOrdinaryScheduleUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val scheduleId: UUID =
        checkNotNull(savedStateHandle[AppRoutes.OrdinaryScheduleDetail.ARG_SCHEDULE_ID])

    val uiState: StateFlow<OrdinaryScheduleDetailUiState> =
        getOrdinaryScheduleByIdUseCase(scheduleId)
            .map { schedule ->
                if (schedule != null) {
                    OrdinaryScheduleDetailUiState.Success(schedule)
                } else {
                    OrdinaryScheduleDetailUiState.Error("未找到 ID 为 $scheduleId 的日程")
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = OrdinaryScheduleDetailUiState.Loading
            )

    // 使用 SharedFlow 发送一次性事件
    private val _eventFlow = MutableSharedFlow<OrdinaryScheduleDetailEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // 添加删除日程的方法
    fun deleteSchedule() {
        viewModelScope.launch {
            try {
                // 从当前状态获取日程 ID，确保日程存在
                val currentState = uiState.value
                if (currentState is OrdinaryScheduleDetailUiState.Success) {
                    deleteOrdinaryScheduleUseCase(currentState.schedule)
                    // 删除成功后发送返回事件
                    _eventFlow.emit(OrdinaryScheduleDetailEvent.NavigateBack)
                } else {
                    // 如果日程不存在或状态错误，发送错误事件
                    _eventFlow.emit(OrdinaryScheduleDetailEvent.ShowError("无法删除：日程未加载或不存在"))
                }
            } catch (e: Exception) {
                // 处理删除过程中的异常
                _eventFlow.emit(OrdinaryScheduleDetailEvent.ShowError("删除日程失败: ${e.message}"))
            }
        }
    }

    // 新增：切换时间段完成状态的方法
    fun toggleTimeSlotCompleted(index: Int) {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is OrdinaryScheduleDetailUiState.Success) {
                val schedule = currentState.schedule
                // 创建新的 timeSlots 列表，切换指定 index 的 isCompleted 状态
                val updatedTimeSlots = schedule.timeSlots.mapIndexed { i, slot ->
                    if (i == index) slot.copy(isCompleted = !slot.isCompleted) else slot
                }
                // 创建新的 schedule
                val updatedSchedule = schedule.copy(timeSlots = updatedTimeSlots)
                // 这里需要调用更新日程的 UseCase（假设有 UpdateOrdinaryScheduleUseCase）
                try {
                    // 需要注入 updateOrdinaryScheduleUseCase
                    updateOrdinaryScheduleUseCase(updatedSchedule)
                } catch (e: Exception) {
                    _eventFlow.emit(OrdinaryScheduleDetailEvent.ShowError("更新时间段状态失败: ${e.message}"))
                }
            }
        }
    }
} 