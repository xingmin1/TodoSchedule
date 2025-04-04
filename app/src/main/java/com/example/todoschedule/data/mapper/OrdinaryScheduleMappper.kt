package com.example.todoschedule.data.mapper

import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.data.database.entity.OrdinaryScheduleEntity
import com.example.todoschedule.data.model.OrdinaryScheduleWithTimeSlots
import com.example.todoschedule.domain.model.OrdinarySchedule

fun OrdinarySchedule.toEntity(): OrdinaryScheduleEntity {
    return OrdinaryScheduleEntity(
        id = this.id,
        title = this.title,
        description = this.description,
        location = this.location,
        category = this.category,
        color = this.color,
        isAllDay = this.isAllDay,
        status = this.status
    )
}

// 从关联模型映射到 Domain 模型， 并过滤时间槽类型
fun OrdinaryScheduleWithTimeSlots.filterAndToDomainModel(): OrdinarySchedule {

    return OrdinarySchedule(
        id = this.schedule.id,
        title = this.schedule.title,
        description = this.schedule.description,
        location = this.schedule.location,
        category = this.schedule.category,
        color = this.schedule.color,
        isAllDay = this.schedule.isAllDay,
        status = this.schedule.status,
        timeSlots = this.timeSlots.filter { it.scheduleType == ScheduleType.ORDINARY }
            .map { it.toDomainModel() }
    )
}
