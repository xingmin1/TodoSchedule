package com.example.todoschedule.ui.ordinaryschedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.use_case.ordinary_schedule.GetOrdinaryScheduleByIdUseCase
import com.example.todoschedule.ui.navigation.AppRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// UI 状态
sealed interface OrdinaryScheduleDetailUiState {
    object Loading : OrdinaryScheduleDetailUiState
    data class Success(val schedule: OrdinarySchedule) : OrdinaryScheduleDetailUiState
    data class Error(val message: String) : OrdinaryScheduleDetailUiState
}

@HiltViewModel
class OrdinaryScheduleDetailViewModel @Inject constructor(
    private val getOrdinaryScheduleByIdUseCase: GetOrdinaryScheduleByIdUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val scheduleId: Int =
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
                initialValue = OrdinaryScheduleDetailUiState.Loading // 初始状态为加载中
            )

    // TODO: Add delete and edit functionality
} 