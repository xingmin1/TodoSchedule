package com.example.todoschedule.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todoschedule.data.database.entity.SyncMessageEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 同步消息数据访问对象
 */
@Dao
interface SyncMessageDao {
    /**
     * 插入一条同步消息
     * @param message 同步消息
     * @return 插入的消息自增ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: SyncMessageEntity): Long

    /**
     * 批量插入同步消息
     * @param messages 同步消息列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<SyncMessageEntity>)

    /**
     * 更新同步消息
     * @param message 同步消息
     */
    @Update
    suspend fun update(message: SyncMessageEntity)

    /**
     * 更新多条同步消息
     * @param messages 同步消息列表
     */
    @Update
    suspend fun updateAll(messages: List<SyncMessageEntity>)

    /**
     * 删除同步消息
     * @param message 同步消息
     */
    @Delete
    suspend fun delete(message: SyncMessageEntity)

    /**
     * 根据消息ID获取同步消息
     * @param syncId 消息ID
     * @return 同步消息
     */
    @Query("SELECT * FROM sync_message WHERE sync_id = :syncId")
    suspend fun getBySyncId(syncId: UUID): SyncMessageEntity?

    /**
     * 获取所有同步消息
     * @return 同步消息列表Flow
     */
    @Query("SELECT * FROM sync_message ORDER BY created_at DESC")
    fun getAllMessages(): Flow<List<SyncMessageEntity>>

    /**
     * 获取待同步的消息列表
     * @return 待同步消息列表
     */
    @Query("SELECT * FROM sync_message WHERE sync_status = 'PENDING' ORDER BY created_at ASC")
    suspend fun getPendingMessages(): List<SyncMessageEntity>

    /**
     * 根据用户ID获取待同步的消息列表
     * @param userId 用户ID
     * @return 待同步消息列表
     */
    @Query("SELECT * FROM sync_message WHERE sync_status = 'PENDING' AND user_id = :userId ORDER BY created_at ASC")
    suspend fun getPendingMessagesByUserId(userId: UUID): List<SyncMessageEntity>

    /**
     * 获取特定类型的待同步消息
     * @param entityType 实体类型
     * @return 待同步消息列表
     */
    @Query("SELECT * FROM sync_message WHERE sync_status = 'PENDING' AND entity_type = :entityType ORDER BY created_at ASC")
    suspend fun getPendingMessagesByType(entityType: String): List<SyncMessageEntity>

    /**
     * 获取特定设备的消息
     * @param deviceId 设备ID
     * @return 同步消息列表
     */
    @Query("SELECT * FROM sync_message WHERE device_id = :deviceId")
    suspend fun getMessagesByDeviceId(deviceId: String): List<SyncMessageEntity>

    /**
     * 获取同步失败的消息
     * @return 同步失败的消息列表
     */
    @Query("SELECT * FROM sync_message WHERE sync_status = 'FAILED'")
    suspend fun getFailedMessages(): List<SyncMessageEntity>

    /**
     * 将特定消息列表标记为已同步
     * @param syncIds 消息ID列表
     */
    @Query("UPDATE sync_message SET sync_status = 'SYNCED' WHERE sync_id IN (:syncIds)")
    suspend fun markAsProcessed(syncIds: List<Int>)

    /**
     * 根据CRDT键和类型获取最新的消息
     * @param crdtKey 实体在分布式系统中的唯一标识
     * @param entityType 实体类型
     * @return 最新的同步消息
     */
    @Query("SELECT * FROM sync_message WHERE crdt_key = :crdtKey AND entity_type = :entityType ORDER BY timestamp_wall_clock DESC, timestamp_logical DESC LIMIT 1")
    suspend fun getLatestMessageForEntity(crdtKey: String, entityType: String): SyncMessageEntity?

    /**
     * 删除已同步的消息
     * @param beforeTimestamp 删除此时间戳之前的消息
     * @return 删除的消息数量
     */
    @Query("DELETE FROM sync_message WHERE sync_status = 'SYNCED' AND created_at < :beforeTimestamp")
    suspend fun deleteProcessedMessages(beforeTimestamp: Long): Int

    /**
     * 清空所有消息
     */
    @Query("DELETE FROM sync_message")
    suspend fun clearAll()
} 