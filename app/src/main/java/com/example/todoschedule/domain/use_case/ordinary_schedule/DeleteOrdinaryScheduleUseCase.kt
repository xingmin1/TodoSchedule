package com.example.todoschedule.domain.use_case.ordinary_schedule

import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.repository.OrdinaryScheduleRepository
import javax.inject.Inject

/**
 * 删除普通日程的用例。
 */
class DeleteOrdinaryScheduleUseCase @Inject constructor(
    private val repository: OrdinaryScheduleRepository
) {
    suspend operator fun invoke(schedule: OrdinarySchedule) {
        repository.deleteSchedule(schedule)
    }
} 