package com.example.todoschedule.domain.use_case.ordinary_schedule

import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.repository.OrdinaryScheduleRepository
import javax.inject.Inject

/**
 * 添加普通日程的用例。
 */
class AddOrdinaryScheduleUseCase @Inject constructor(
    private val repository: OrdinaryScheduleRepository
) {
    suspend operator fun invoke(schedule: OrdinarySchedule): Long {
        // 在这里可以添加额外的业务逻辑，例如验证
        if (schedule.title.isBlank()) {
            throw IllegalArgumentException("日程标题不能为空")
        }
        return repository.insertSchedule(schedule)
    }
} 