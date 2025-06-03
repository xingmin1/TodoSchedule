package com.example.todoschedule.data.mapper

import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.data.database.entity.TimeSlotEntity
import com.example.todoschedule.domain.model.TimeSlot
import java.util.UUID


fun TimeSlot.toEntity(scheduleId: UUID, scheduleType: ScheduleType): TimeSlotEntity {
    return TimeSlotEntity(
        id = this.id,
        startTime = this.startTime,
        endTime = this.endTime,
        scheduleType = scheduleType, // 使用传入的类型
        scheduleId = scheduleId,   // 使用传入的 ID
        head = this.head,
        priority = this.priority,
        isCompleted = this.isCompleted,
        isRepeated = this.isRepeated,
        repeatPattern = this.repeatPattern,
        reminderType = this.reminderType,
        reminderOffset = this.reminderOffset,
        userId = this.userId,
    )
}

fun TimeSlotEntity.toDomainModel(): TimeSlot {
    return TimeSlot(
        id = this.id,
        startTime = this.startTime,
        endTime = this.endTime,
        scheduleType = this.scheduleType,
        scheduleId = this.scheduleId,
        head = this.head,
        priority = this.priority,
        isCompleted = this.isCompleted,
        isRepeated = this.isRepeated,
        repeatPattern = this.repeatPattern,
        reminderType = this.reminderType,
        reminderOffset = this.reminderOffset,
        userId = this.userId,
    )
}
