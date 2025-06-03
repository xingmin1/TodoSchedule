package com.example.todoschedule.data.repository

// 移除不必要的导入
import android.util.Log
import androidx.room.Transaction
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.data.database.dao.OrdinaryScheduleDao
import com.example.todoschedule.data.database.dao.TimeSlotDao
import com.example.todoschedule.data.mapper.filterAndToDomainModel
import com.example.todoschedule.data.mapper.toEntity
import com.example.todoschedule.data.sync.SyncConstants
import com.example.todoschedule.data.sync.SyncManager
import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.repository.OrdinaryScheduleRepository
import com.example.todoschedule.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/**
 * OrdinaryScheduleRepository 的实现类。
 * 负责处理普通日程及其关联时间槽的数据操作，并进行数据模型之间的转换。
 * 使用 Room 的 @Relation 来优化查询。
 * 同时支持创建同步消息并上传到服务器。
 */
class OrdinaryScheduleRepositoryImpl @Inject constructor(
    private val ordinaryScheduleDao: OrdinaryScheduleDao,
    private val timeSlotDao: TimeSlotDao,
    private val sessionRepository: SessionRepository,
    private val syncManager: SyncManager
) : OrdinaryScheduleRepository {

    private val TAG = "OrdinaryScheduleRepo"

    // 常量定义日程类型
    private val scheduleType = ScheduleType.ORDINARY

    // 使用 @Transaction 确保插入日程和时间槽的原子性
    @Transaction
    override suspend fun insertSchedule(schedule: OrdinarySchedule): UUID {
        try {
            // 1. 插入 OrdinaryScheduleEntity 并获取 ID
            val scheduleEntity = schedule.toEntity()
            val scheduleId = scheduleEntity.id
            assert(scheduleId != AppConstants.EMPTY_UUID) { "Schedule ID must not be zero" }
            ordinaryScheduleDao.insertSchedule(scheduleEntity)

            // 设置scheduleId后的实体
            val updatedEntity = scheduleEntity.copy(id = scheduleId)

            Log.d(TAG, "成功插入日程: ${updatedEntity.id}")

            // 2. 准备 TimeSlotEntity 列表 (设置 scheduleId 和 scheduleType)
            val timeSlotEntities = schedule.timeSlots.map {
                // 确保传入正确的 ID
                it.toEntity(scheduleId, scheduleType)
            }

            // 3. 插入 TimeSlotEntity 列表
            if (timeSlotEntities.isNotEmpty()) {
                timeSlotDao.insertTimeSlots(timeSlotEntities)
            }
            Log.d(TAG, "成功插入时间槽，数量: ${timeSlotEntities.size}")

            // 4. 创建日程的同步消息
            val userId = sessionRepository.currentUserIdFlow.first() ?: schedule.userId
            syncManager.createAndSaveSyncMessage(
                entityType = SyncConstants.EntityType.ORDINARY_SCHEDULE,
                operationType = SyncConstants.OperationType.ADD,
                userId = userId,
                entity = updatedEntity
            )
            Log.d(TAG, "日程已创建同步消息: ${updatedEntity.id}")

            // 5. 创建每个时间槽的同步消息
            for (timeSlotEntity in timeSlotEntities) {
                syncManager.createAndSaveSyncMessage(
                    entityType = SyncConstants.EntityType.TIME_SLOT,
                    operationType = SyncConstants.OperationType.ADD,
                    userId = userId,
                    entity = timeSlotEntity
                )
            }
            Log.d(TAG, "已为${timeSlotEntities.count()}个时间槽创建同步消息")

            // 6. 主动触发同步
            try {
                Log.d(TAG, "正在主动触发同步过程来上传日程和时间槽...")
                syncManager.syncNow(ignoreExceptions = true)
                Log.d(TAG, "主动触发同步完成")
            } catch (e: Exception) {
                Log.e(TAG, "主动触发同步失败: ${e.message}", e)
                // 同步失败不影响数据添加的结果
            }

            return scheduleId
        } catch (e: Exception) {
            Log.e(TAG, "插入日程及同步消息失败", e)
            throw e
        }
    }

    @Transaction
    override suspend fun insertSchedules(schedules: List<OrdinarySchedule>) {
        try {
            // 批量插入日程实体
            val scheduleEntities = schedules.map { it.toEntity() }
            val scheduleIds = scheduleEntities.map { it.id }
            ordinaryScheduleDao.insertSchedules(scheduleEntities)

            // 准备并批量插入时间槽实体
            val timeSlotEntities = schedules.flatMapIndexed { index, schedule ->
                // 获取日程实体及crdt键
                val scheduleEntity = scheduleEntities[index]
                val scheduleCrdtKey = scheduleEntity.id
                Log.d(TAG, "【日程CRDT键】日程'${scheduleEntity.title}'的CRDT键: $scheduleCrdtKey")

                schedule.timeSlots.map { timeSlot ->
                    val entity = timeSlot.toEntity(scheduleIds[index], scheduleType)
                    // 使用新增的scheduleCrdtKey字段设置关联的日程CRDT键
                    entity.copy(scheduleId = scheduleCrdtKey)
                }
            }
            if (timeSlotEntities.isNotEmpty()) {
                timeSlotDao.insertTimeSlots(timeSlotEntities)
            }

            // 获取当前用户ID
            val userId = sessionRepository.currentUserIdFlow.first()
                ?: (schedules.firstOrNull()?.userId ?: AppConstants.EMPTY_UUID)

            // 为每个日程创建同步消息
            scheduleEntities.forEachIndexed { index, entity ->
                // 更新实体ID，确保使用数据库分配的ID
                val updatedEntity = entity.copy(id = scheduleIds[index])

                syncManager.createAndSaveSyncMessage(
                    entityType = SyncConstants.EntityType.ORDINARY_SCHEDULE,
                    operationType = SyncConstants.OperationType.ADD,
                    userId = userId,
                    entity = updatedEntity
                )
            }
            Log.d(TAG, "已为${scheduleEntities.size}个日程创建同步消息")

            // 为每个时间槽创建同步消息
            timeSlotEntities.forEach { timeSlotEntity ->
                syncManager.createAndSaveSyncMessage(
                    entityType = SyncConstants.EntityType.TIME_SLOT,
                    operationType = SyncConstants.OperationType.ADD,
                    userId = userId,
                    entity = timeSlotEntity
                )
            }
            Log.d(TAG, "已为${timeSlotEntities.size}个时间槽创建同步消息")

            // 主动触发同步
            try {
                Log.d(TAG, "正在主动触发同步过程来上传批量日程和时间槽...")
                syncManager.syncNow(ignoreExceptions = true)
                Log.d(TAG, "主动触发同步完成")
            } catch (e: Exception) {
                Log.e(TAG, "主动触发同步失败: ${e.message}", e)
                // 同步失败不影响数据添加的结果
            }
        } catch (e: Exception) {
            Log.e(TAG, "批量插入日程及同步消息失败", e)
            throw e
        }
    }

    @Transaction
    override suspend fun updateSchedule(schedule: OrdinarySchedule) {
        try {
            // 1. 更新 OrdinaryScheduleEntity
            val scheduleEntity = schedule.toEntity()
            ordinaryScheduleDao.updateSchedule(scheduleEntity)

            // 2. 删除与此日程关联的旧的 TimeSlotEntity
            // 注意：这里仍然需要手动删除，@Relation 主要用于查询
            val oldTimeSlots = timeSlotDao.getTimeSlotsBySchedule(scheduleType,
                schedule.id.toString()
            ).first()
            for (oldTimeSlot in oldTimeSlots) {
                timeSlotDao.deleteTimeSlot(oldTimeSlot)
            }

            // 3. 插入新的 TimeSlotEntity (设置 scheduleId、scheduleType 和 scheduleCrdtKey)
            val scheduleCrdtKey = scheduleEntity.id
            Log.d(TAG, "【更新日程】日程'${scheduleEntity.title}'的CRDT键: $scheduleCrdtKey")

            val timeSlotEntities = schedule.timeSlots.map {
                // 确保使用正确的 schedule.id 和 scheduleCrdtKey
                val entity = it.toEntity(schedule.id, scheduleType)
                entity.copy(scheduleId = scheduleCrdtKey)
            }
            if (timeSlotEntities.isNotEmpty()) {
                timeSlotDao.insertTimeSlots(timeSlotEntities)
            }

            // 4. 创建日程更新的同步消息
            val userId = sessionRepository.currentUserIdFlow.first() ?: schedule.userId
            syncManager.createAndSaveSyncMessage(
                entityType = SyncConstants.EntityType.ORDINARY_SCHEDULE,
                operationType = SyncConstants.OperationType.UPDATE,
                userId = userId,
                entity = scheduleEntity
            )
            Log.d(TAG, "日程更新已创建同步消息: ${scheduleEntity.id}")

            // 5. 创建每个新时间槽的同步消息
            timeSlotEntities.forEach { timeSlotEntity ->
                syncManager.createAndSaveSyncMessage(
                    entityType = SyncConstants.EntityType.TIME_SLOT,
                    operationType = SyncConstants.OperationType.ADD, // 新插入的时间槽用ADD
                    userId = userId,
                    entity = timeSlotEntity
                )
            }
            Log.d(TAG, "已为${timeSlotEntities.size}个新时间槽创建同步消息")

            // 6. 主动触发同步
            try {
                Log.d(TAG, "正在主动触发同步过程来上传日程更新和时间槽...")
                syncManager.syncNow(ignoreExceptions = true)
                Log.d(TAG, "主动触发同步完成")
            } catch (e: Exception) {
                Log.e(TAG, "主动触发同步失败: ${e.message}", e)
                // 同步失败不影响数据更新的结果
            }
        } catch (e: Exception) {
            Log.e(TAG, "更新日程及同步消息失败", e)
            throw e
        }
    }

    @Transaction
    override suspend fun deleteSchedule(schedule: OrdinarySchedule) {
        try {
            // 获取时间槽实体，用于创建删除同步消息
            // getTimeSlotsBySchedule返回的是Flow，需要先收集数据
            val timeSlotEntities =
                timeSlotDao.getTimeSlotsBySchedule(scheduleType, schedule.id.toString()).first()

            // 获取用户ID
            val userId = sessionRepository.currentUserIdFlow.first() ?: schedule.userId

            // 为每个时间槽创建删除同步消息
            for (timeSlotEntity in timeSlotEntities) {
                syncManager.createAndSaveSyncMessage(
                    entityType = SyncConstants.EntityType.TIME_SLOT,
                    operationType = SyncConstants.OperationType.DELETE,
                    userId = userId,
                    entity = timeSlotEntity
                )
            }
            Log.d(TAG, "已为${timeSlotEntities.count()}个时间槽创建删除同步消息")

            // 为日程创建删除同步消息
            val scheduleEntity = schedule.toEntity()
            syncManager.createAndSaveSyncMessage(
                entityType = SyncConstants.EntityType.ORDINARY_SCHEDULE,
                operationType = SyncConstants.OperationType.DELETE,
                userId = userId,
                entity = scheduleEntity
            )
            Log.d(TAG, "日程删除已创建同步消息: ${scheduleEntity.id}")

            // 执行删除操作
            // 1. 删除关联的 TimeSlots
            timeSlotDao.deleteTimeSlotsBySchedule(scheduleType, schedule.id.toString())
            // 2. 删除 OrdinaryScheduleEntity
            ordinaryScheduleDao.deleteSchedule(scheduleEntity)

            // 主动触发同步
            try {
                Log.d(TAG, "正在主动触发同步过程来上传删除同步消息...")
                syncManager.syncNow(ignoreExceptions = true)
                Log.d(TAG, "主动触发同步完成")
            } catch (e: Exception) {
                Log.e(TAG, "主动触发同步失败: ${e.message}", e)
                // 同步失败不影响数据删除的结果
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除日程及同步消息失败", e)
            throw e
        }
    }

    override fun getScheduleById(id: UUID): Flow<OrdinarySchedule?> {
        // 使用新的 DAO 方法
        return ordinaryScheduleDao.getScheduleWithTimeSlotsById(id).map { scheduleWithSlots ->
            // 映射到 Domain Model
            scheduleWithSlots?.filterAndToDomainModel()
        }
    }

    override fun getAllSchedules(userId: UUID): Flow<List<OrdinarySchedule>> {
        // 使用新的 DAO 方法
        Log.d(TAG, "获取用户 $userId 的所有日程")
        return ordinaryScheduleDao.getAllSchedulesWithTimeSlots(userId).map { list ->
            Log.d(TAG, "从数据库获取到 ${list.size} 个日程实体与时间槽")

            list.forEach { scheduleWithSlots ->
                Log.d(
                    TAG,
                    "日程: ID=${scheduleWithSlots.schedule.id}, 标题='${scheduleWithSlots.schedule.title}', 状态=${scheduleWithSlots.schedule.status}, 时间槽数=${scheduleWithSlots.timeSlots.size}"
                )
                scheduleWithSlots.timeSlots.forEach { slot ->
                    Log.d(
                        TAG,
                        "  时间槽: ID=${slot.id}, 开始时间=${slot.startTime}, 结束时间=${slot.endTime}"
                    )
                }
            }

            val result = list.map { scheduleWithSlots ->
                // 映射到 Domain Model，在映射中过滤时间槽类型
                scheduleWithSlots
                    .filterAndToDomainModel()
            }

            Log.d(TAG, "转换后返回 ${result.size} 个 Domain 模型日程")
            result
        }
    }

    @Transaction
    override suspend fun deleteAllSchedules(userId: UUID) {
        try {
            // 1. 获取所有普通日程及其时间槽
            val allSchedulesWithSlots = ordinaryScheduleDao.getAllSchedulesWithTimeSlots(userId)
                .first()

            // 如果没有日程，直接返回
            if (allSchedulesWithSlots.isEmpty()) {
                Log.d(TAG, "没有找到需要删除的日程")
                return
            }

            // 提取所有日程 ID
            val allScheduleIds = allSchedulesWithSlots.map { it.schedule.id }

            // 2. 为每个日程和时间槽创建删除同步消息
            for (scheduleWithSlots in allSchedulesWithSlots) {
                // 为日程创建删除同步消息
                syncManager.createAndSaveSyncMessage(
                    entityType = SyncConstants.EntityType.ORDINARY_SCHEDULE,
                    operationType = SyncConstants.OperationType.DELETE,
                    userId = userId,
                    entity = scheduleWithSlots.schedule
                )

                // 为每个时间槽创建删除同步消息
                for (timeSlot in scheduleWithSlots.timeSlots) {
                    syncManager.createAndSaveSyncMessage(
                        entityType = SyncConstants.EntityType.TIME_SLOT,
                        operationType = SyncConstants.OperationType.DELETE,
                        userId = userId,
                        entity = timeSlot
                    )
                }
            }

            val totalSchedules = allSchedulesWithSlots.size
            val totalTimeSlots = allSchedulesWithSlots.sumOf { it.timeSlots.count() }
            Log.d(TAG, "已为${totalSchedules}个日程和${totalTimeSlots}个时间槽创建删除同步消息")

            // 3. 删除所有关联的 TimeSlots
            for (id in allScheduleIds) {
                timeSlotDao.deleteTimeSlotsBySchedule(scheduleType, id.toString())
            }

            // 4. 删除所有普通日程
            ordinaryScheduleDao.deleteAllSchedules(userId)

            // 5. 主动触发同步
            try {
                Log.d(TAG, "正在主动触发同步过程来上传批量删除同步消息...")
                syncManager.syncNow(ignoreExceptions = true)
                Log.d(TAG, "主动触发同步完成")
            } catch (e: Exception) {
                Log.e(TAG, "主动触发同步失败: ${e.message}", e)
                // 同步失败不影响数据删除的结果
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除所有日程及同步消息失败", e)
            throw e
        }
    }
}