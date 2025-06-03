package com.example.todoschedule.data.repository

import com.example.todoschedule.data.database.dao.CourseDao
import com.example.todoschedule.data.database.dao.CourseNodeDao
import com.example.todoschedule.data.database.dao.OrdinaryScheduleDao
import com.example.todoschedule.data.database.dao.TableDao
import com.example.todoschedule.data.database.entity.SyncMessageEntity
import com.example.todoschedule.data.sync.SyncConstants
import com.example.todoschedule.data.sync.dto.SyncMessageDto
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 同步仓库接口
 */
interface SyncRepository {
    /**
     * 获取课程DAO
     */
    fun getCourseDao(): CourseDao

    /**
     * 获取课程节点DAO
     */
    fun getCourseNodeDao(): CourseNodeDao

    /**
     * 获取课表DAO
     */
    fun getTableDao(): TableDao

    /**
     * 获取普通日程DAO
     */
    fun getOrdinaryScheduleDao(): OrdinaryScheduleDao

    /**
     * 保存同步消息到本地数据库
     * @param message 同步消息
     */
    suspend fun saveSyncMessage(message: SyncMessageEntity)

    /**
     * 批量保存同步消息到本地数据库
     * @param messages 同步消息列表
     */
    suspend fun saveSyncMessages(messages: List<SyncMessageEntity>)

    /**
     * 获取待同步的消息列表
     * @return 待同步消息列表
     */
    suspend fun getPendingMessages(): List<SyncMessageEntity>

    /**
     * 根据用户ID获取待同步的消息列表
     * @param userId 用户ID
     * @return 待同步消息列表
     */
    suspend fun getPendingMessagesByUserId(userId: UUID): List<SyncMessageEntity>

    /**
     * 获取特定类型的待同步消息
     * @param entityType 实体类型
     * @return 待同步消息列表
     */
    suspend fun getPendingMessagesByType(entityType: String): List<SyncMessageEntity>

    /**
     * u4e0au4f20u540cu6b65u6d88u606fu5230u670du52a1u5668
     * @param messages u540cu6b65u6d88u606f5217u8868
     * @param entityType u5b9eu4f53u7c7bu578b
     * @return u4e0au4f20u6210u529fu7684u6d88u606fIDu5217u8868uff08u5b57u7b26u4e32u8868u793au7684syncIduff09
     */
    suspend fun uploadMessages(messages: List<SyncMessageEntity>, entityType: String): List<String>

    /**
     * 从服务器下载所有同步消息
     * @return 下载的同步消息列表
     */
    suspend fun downloadAllMessages(): List<SyncMessageDto>

    /**
     * 从服务器下载指定类型的同步消息
     * @param entityType 实体类型
     * @return 下载的同步消息列表
     */
    suspend fun downloadMessagesByEntityType(entityType: String): List<SyncMessageDto>

    /**
     * 从服务器下载所有非本设备的同步消息
     * @return 下载的同步消息列表
     */
    suspend fun downloadAllMessagesExcludeOrigin(): List<SyncMessageDto>

    /**
     * 从服务器下载指定类型的非本设备同步消息
     * @param entityType 实体类型
     * @return 下载的同步消息列表
     */
    suspend fun downloadMessagesByEntityTypeExcludeOrigin(entityType: String): List<SyncMessageDto>

    /**
     * 将同步消息标记为已处理
     * @param ids 消息ID列表
     */
    suspend fun markMessagesAsProcessed(ids: List<Int>)

    /**
     * 将同步消息标记为已同步
     * @param ids 消息ID列表
     */
    suspend fun markMessagesAsSynced(ids: List<Int>)

    /**
     * 注册设备
     * @param userId 用户ID
     * @return 是否注册成功
     */
    suspend fun registerDevice(userId: UUID): Boolean

    /**
     * 获取所有同步消息
     * @return 所有同步消息的Flow
     */
    fun getAllSyncMessages(): Flow<List<SyncMessageEntity>>

    /**
     * 同步特定类型的实体
     * @param entityType 实体类型
     * @return 是否同步成功
     */
    suspend fun syncEntityType(entityType: SyncConstants.EntityType): Boolean

    /**
     * 同步所有类型的实体
     * @return 是否全部同步成功
     */
    suspend fun syncAll(): Boolean

    /**
     * 清理旧消息
     * @param beforeTime 清理此时间之前的消息
     * @return 已清理的消息数量
     */
    suspend fun cleanupOldMessages(beforeTime: Long): Int

    /**
     * 从会话仓库获取token
     */
    suspend fun getTokenFromSession(): String?

    /**
     * 从会话仓库获取当前用户ID
     */
    suspend fun getUserIdFromSession(): Long?

    /**
     * 执行完整的数据同步流程
     */
    suspend fun syncData()

    /**
     * 根据CRDT键获取实体
     *
     * @param crdtKey 实体的CRDT键
     * @return 匹配的实体或null
     */
    suspend fun <T> getEntityByCrdtKey(crdtKey: String): T?

    /**
     * 保存实体
     *
     * @param entity 要保存的实体
     * @return 保存是否成功
     */
    suspend fun <T> saveEntity(entity: T): Boolean
} 