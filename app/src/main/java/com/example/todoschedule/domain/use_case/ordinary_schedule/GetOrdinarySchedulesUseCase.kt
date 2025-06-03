package com.example.todoschedule.domain.use_case.ordinary_schedule

import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.repository.OrdinaryScheduleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取所有普通日程的用例。
 */
class GetOrdinarySchedulesUseCase @Inject constructor(
    private val repository: OrdinaryScheduleRepository
) {
    operator fun invoke(userId: UUID): Flow<List<OrdinarySchedule>> {
        return repository.getAllSchedules(userId)
    }
} 