package com.example.todoschedule.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.todoschedule.data.database.entity.OrdinaryScheduleEntity
import com.example.todoschedule.data.database.entity.TimeSlotEntity

/**
 * 用于 Room 查询的关联模型，包含一个普通日程及其所有关联的时间槽。
 * 注意：此关联基于 scheduleId 匹配，并未在此直接过滤 scheduleType。
 *       确保 DAO 查询或 Repository 映射时处理类型过滤。
 */
data class OrdinaryScheduleWithTimeSlots(
    @Embedded val schedule: OrdinaryScheduleEntity,
    @Relation(
        parentColumn = "id", // OrdinaryScheduleEntity 的主键
        entityColumn = "schedule_id" // TimeSlotEntity 的外键字段
        // entity = TimeSlotEntity::class // 可选，显式指定子实体类型
    )
    val timeSlots: List<TimeSlotEntity> = emptyList()
)