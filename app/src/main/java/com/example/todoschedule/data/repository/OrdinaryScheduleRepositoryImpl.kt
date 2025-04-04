package com.example.todoschedule.data.repository

import androidx.room.Transaction
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.data.database.dao.OrdinaryScheduleDao
import com.example.todoschedule.data.database.dao.TimeSlotDao
import com.example.todoschedule.data.mapper.filterAndToDomainModel
import com.example.todoschedule.data.mapper.toEntity
import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.repository.OrdinaryScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * OrdinaryScheduleRepository 的实现类。
 * 负责处理普通日程及其关联时间槽的数据操作，并进行数据模型之间的转换。
 * 使用 Room 的 @Relation 来优化查询。
 */
class OrdinaryScheduleRepositoryImpl @Inject constructor(
    private val ordinaryScheduleDao: OrdinaryScheduleDao,
    private val timeSlotDao: TimeSlotDao
) : OrdinaryScheduleRepository {

    // 常量定义日程类型
    private val scheduleType = ScheduleType.ORDINARY

    // 使用 @Transaction 确保插入日程和时间槽的原子性
    @Transaction
    override suspend fun insertSchedule(schedule: OrdinarySchedule): Long {
        // 1. 插入 OrdinaryScheduleEntity 并获取 ID
        val scheduleEntity = schedule.toEntity()
        val scheduleId = ordinaryScheduleDao.insertSchedule(scheduleEntity)

        // 2. 准备 TimeSlotEntity 列表 (设置 scheduleId 和 scheduleType)
        val timeSlotEntities = schedule.timeSlots.map {
            // 确保传入正确的 ID
            it.toEntity(scheduleId.toInt(), scheduleType)
        }

        // 3. 插入 TimeSlotEntity 列表
        if (timeSlotEntities.isNotEmpty()) {
            timeSlotDao.insertTimeSlots(timeSlotEntities)
        }
        return scheduleId
    }

    @Transaction
    override suspend fun insertSchedules(schedules: List<OrdinarySchedule>) {
        // 可以优化为先批量插入 schedules，获取 IDs，再批量插入 time slots
        // schedules.forEach { insertSchedule(it) } // 保持简单实现，但标记为可优化
        val scheduleEntities = schedules.map { it.toEntity() }
        val scheduleIds = ordinaryScheduleDao.insertSchedules(scheduleEntities)
        val timeSlotEntities = schedules.flatMapIndexed { index, schedule ->
            schedule.timeSlots.map { timeSlot ->
                timeSlot.toEntity(scheduleIds[index].toInt(), scheduleType)
            }
        }
        if (timeSlotEntities.isNotEmpty()) {
            timeSlotDao.insertTimeSlots(timeSlotEntities)
        }
    }

    @Transaction
    override suspend fun updateSchedule(schedule: OrdinarySchedule) {
        // 1. 更新 OrdinaryScheduleEntity
        val scheduleEntity = schedule.toEntity()
        ordinaryScheduleDao.updateSchedule(scheduleEntity)

        // 2. 删除与此日程关联的旧的 TimeSlotEntity
        // 注意：这里仍然需要手动删除，@Relation 主要用于查询
        timeSlotDao.deleteTimeSlotsBySchedule(scheduleType, schedule.id)

        // 3. 插入新的 TimeSlotEntity (设置 scheduleId 和 scheduleType)
        val timeSlotEntities = schedule.timeSlots.map {
            // 确保使用正确的 schedule.id
            it.toEntity(schedule.id, scheduleType)
        }
        if (timeSlotEntities.isNotEmpty()) {
            timeSlotDao.insertTimeSlots(timeSlotEntities)
        }
    }

    @Transaction
    override suspend fun deleteSchedule(schedule: OrdinarySchedule) {
        // 1. 删除关联的 TimeSlots
        timeSlotDao.deleteTimeSlotsBySchedule(scheduleType, schedule.id)
        // 2. 删除 OrdinaryScheduleEntity
        ordinaryScheduleDao.deleteSchedule(schedule.toEntity())
    }

    override fun getScheduleById(id: Int): Flow<OrdinarySchedule?> {
        // 使用新的 DAO 方法
        return ordinaryScheduleDao.getScheduleWithTimeSlotsById(id).map { scheduleWithSlots ->
            // 映射到 Domain Model
            scheduleWithSlots?.filterAndToDomainModel()
        }
    }

    override fun getAllSchedules(): Flow<List<OrdinarySchedule>> {
        // 使用新的 DAO 方法
        return ordinaryScheduleDao.getAllSchedulesWithTimeSlots().map { list ->
            list.map { scheduleWithSlots ->
                // 映射到 Domain Model，在映射中过滤时间槽类型
                scheduleWithSlots
                    .filterAndToDomainModel()
            }
        }
    }

    @Transaction
    override suspend fun deleteAllSchedules() {
        // 1. 获取所有普通日程的 ID (使用新的 DAO 方法，或者直接查询 IDs)
        //   由于 @Relation 返回的是 Flow<List<...>>，直接获取 ID 列表可能需要 .first()
        //   或者在 DAO 中添加一个仅查询 ID 的方法。
        //   为了简单起见，我们仍然可以查询整个对象列表，然后提取 ID。
        val allScheduleIds = ordinaryScheduleDao.getAllSchedulesWithTimeSlots()
            .map { list -> list.map { it.schedule.id } } // 映射到 ID 列表的 Flow
            .firstOrNull() ?: emptyList() // 获取第一个列表或空列表

        // 2. 删除所有关联的 TimeSlots
        allScheduleIds.forEach { id ->
            timeSlotDao.deleteTimeSlotsBySchedule(scheduleType, id)
        }

        // 3. 删除所有普通日程
        ordinaryScheduleDao.deleteAllSchedules()
    }
}