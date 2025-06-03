package com.example.todoschedule.domain.use_case.ordinary_schedule

import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.repository.OrdinaryScheduleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 根据 ID 获取单个普通日程的用例。
 */
class GetOrdinaryScheduleByIdUseCase @Inject constructor(
    private val repository: OrdinaryScheduleRepository
) {
    operator fun invoke(Id: UUID): Flow<OrdinarySchedule?> {
        return repository.getScheduleById(id)
    }
} 