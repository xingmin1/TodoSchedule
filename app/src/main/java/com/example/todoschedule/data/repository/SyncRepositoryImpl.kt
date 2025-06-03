package com.example.todoschedule.data.repository

import android.util.Log
import com.example.todoschedule.core.extensions.valid
import com.example.todoschedule.data.database.AppDatabase
import com.example.todoschedule.data.database.dao.CourseDao
import com.example.todoschedule.data.database.dao.CourseNodeDao
import com.example.todoschedule.data.database.dao.OrdinaryScheduleDao
import com.example.todoschedule.data.database.dao.SyncMessageDao
import com.example.todoschedule.data.database.dao.TableDao
import com.example.todoschedule.data.database.entity.CourseEntity
import com.example.todoschedule.data.database.entity.CourseNodeEntity
import com.example.todoschedule.data.database.entity.OrdinaryScheduleEntity
import com.example.todoschedule.data.database.entity.SyncMessageEntity
import com.example.todoschedule.data.database.entity.TableEntity
import com.example.todoschedule.data.database.entity.TimeSlotEntity
import com.example.todoschedule.data.database.entity.toEntity
import com.example.todoschedule.data.remote.api.SyncApi
import com.example.todoschedule.data.sync.DeviceIdManager
import com.example.todoschedule.data.sync.SyncConstants
import com.example.todoschedule.data.sync.SyncManager
import com.example.todoschedule.data.sync.dto.DeviceRegistrationDto
import com.example.todoschedule.data.sync.dto.SyncMessageDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 同步仓库实现类
 */
@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val syncMessageDao: SyncMessageDao,
    private val syncApi: SyncApi,
    private val deviceIdManager: DeviceIdManager,
    private val database: AppDatabase,
    private val sessionRepository: com.example.todoschedule.domain.repository.SessionRepository,
    private val syncManagerProvider: Provider<SyncManager>,
    private val globalSettingRepository: com.example.todoschedule.domain.repository.GlobalSettingRepository
) : SyncRepository {

    private val TAG = "SyncRepository"
    private val json = Json { encodeDefaults = true }

    // 获取SyncManager实例时使用provider.get()，延迟加载
    private val syncManager: SyncManager
        get() = syncManagerProvider.get()

    /**
     * 获取课程DAO
     */
    override fun getCourseDao(): CourseDao {
        return database.courseDao()
    }

    /**
     * 获取课程节点DAO
     */
    override fun getCourseNodeDao(): CourseNodeDao {
        return database.courseNodeDao()
    }

    /**
     * 获取课表DAO
     */
    override fun getTableDao(): TableDao {
        return database.tableDao()
    }

    /**
     * 获取普通日程DAO
     */
    override fun getOrdinaryScheduleDao(): OrdinaryScheduleDao {
        return database.ordinaryScheduleDao()
    }

    override suspend fun saveSyncMessage(message: SyncMessageEntity) {
        syncMessageDao.insert(message)
    }

    override suspend fun saveSyncMessages(messages: List<SyncMessageEntity>) {
        syncMessageDao.insertAll(messages)
    }

    override suspend fun getPendingMessages(): List<SyncMessageEntity> {
        return syncMessageDao.getPendingMessages()
    }

    override suspend fun getPendingMessagesByUserId(userId: UUID): List<SyncMessageEntity> {
        return syncMessageDao.getPendingMessagesByUserId(userId)
    }

    override suspend fun getPendingMessagesByType(entityType: String): List<SyncMessageEntity> {
        return syncMessageDao.getPendingMessagesByType(entityType)
    }

    /**
     * 判断是否是可重试的HTTP状态码
     */
    private fun isRetryableHttpCode(code: Int): Boolean {
        // 5xx服务器错误通常可以重试
        // 429 Too Many Requests也可以重试
        // 部分408 Request Timeout可以重试
        return code >= 500 || code == 429 || code == 408
    }

    /**
     * 判断是否是可重试的错误消息
     */
    private fun isRetryableError(errorMsg: String): Boolean {
        // 包含这些关键词的错误通常可以重试
        val retryableKeywords = listOf(
            "timeout", "超时",
            "connection", "连接",
            "temporary", "暂时",
            "overloaded", "过载",
            "try again", "重试"
        )

        return retryableKeywords.any { errorMsg.contains(it, ignoreCase = true) }
    }

    override suspend fun uploadMessages(
        messages: List<SyncMessageEntity>,
        entityType: String
    ): List<String> {
        if (messages.isEmpty()) {
            Log.d(TAG, "没有待上传的消息，entityType: $entityType")
            return emptyList()
        }

        Log.d(TAG, "======== 开始上传同步消息 ========")
        Log.d(TAG, "消息数量: ${messages.size}, 实体类型: $entityType")
        Log.d(TAG, "当前线程: ${Thread.currentThread().name}")

        // 打印服务器基本信息
        val baseUrl = com.example.todoschedule.core.constants.AppConstants.Api.BASE_URL
        Log.d(TAG, "API基础URL: $baseUrl")

        // 尝试获取用户信息和认证状态
        val userId = getUserIdFromSession()
        val token = getTokenFromSession()
        Log.d(TAG, "当前用户ID: $userId, 认证Token存在: ${!token.isNullOrEmpty()}")

        try {
            val deviceId = deviceIdManager.getOrCreateDeviceId()
            Log.d(TAG, "设备ID: $deviceId")

            // 记录一些原始消息的关键信息
            messages.take(3).forEachIndexed { index, entity ->
                Log.d(
                    TAG,
                    "原始消息[$index] - ID: ${entity.id}, 类型: ${entity.entityType}, 操作: ${entity.operationType}"
                )
                Log.d(
                    TAG,
                    "  -> 时间戳: wall=${entity.timestampWallClock}, logical=${entity.timestampLogical}, node=${entity.timestampNodeId}"
                )
                Log.d(TAG, "  -> 状态: ${entity.syncStatus}, 错误: ${entity.syncError ?: "None"}")
                Log.d(TAG, "  -> 负载长度: ${entity.payload.length} 字符")
            }
            if (messages.size > 3) {
                Log.d(TAG, "... 还有${messages.size - 3}条消息 ...")
            }

            Log.d(TAG, "开始转换为DTO...")
            val messageDtos = try {
                messages.map { it.toDto() }
            } catch (e: Exception) {
                Log.e(TAG, "消息转换为DTO失败: ${e::class.java.simpleName}: ${e.message}", e)
                throw e
            }

            // 验证DTO转换正确性
            messageDtos.take(3).forEachIndexed { index, dto ->
                Log.d(
                    TAG,
                    "DTO[$index] - 类型: ${dto.entityType}, 操作: ${dto.operationType}, CRDT键: ${dto.id}"
                )
                Log.d(
                    TAG,
                    "  -> 时间戳: wall=${dto.timestamp.wallClockTime}, logical=${dto.timestamp.logicalTime}, node=${dto.timestamp.nodeId}"
                )
                Log.d(TAG, "  -> 设备ID: ${dto.deviceId}, 用户ID: ${dto.userId}")
                Log.d(TAG, "  -> 负载长度: ${dto.payload.length} 字符")
            }

            // 将DTO对象转换为JSON字符串
            Log.d(TAG, "开始序列化DTO为JSON...")
            val serializedMessages = try {
                messageDtos.map { json.encodeToString(it) }
            } catch (e: Exception) {
                Log.e(TAG, "DTO序列化为JSON失败: ${e::class.java.simpleName}: ${e.message}", e)
                throw e
            }

            Log.d(
                TAG,
                "准备上传${messages.size}条同步消息，实体类型: $entityType，设备ID: $deviceId，第一条消息ID: ${messages.firstOrNull()?.id}"
            )
            if (serializedMessages.isNotEmpty()) {
                val firstMessage = serializedMessages.first()
                Log.d(TAG, "第一条消息序列化后: ${firstMessage.take(100)}...")
                Log.d(TAG, "第一条消息总长度: ${firstMessage.length} 字符")

                // 分析JSON结构
                try {
                    val firstJson = org.json.JSONObject(firstMessage)
                    Log.d(TAG, "JSON字段: ${firstJson.keys().asSequence().toList()}")

                    // 检查时间戳格式
                    if (firstJson.has("hlcTimestamp")) {
                        val timestamp = firstJson.get("hlcTimestamp")
                        Log.d(
                            TAG,
                            "时间戳格式: ${timestamp::class.java.simpleName}, 值: $timestamp"
                        )
                    }

                    // 检查必须字段
                    val requiredFields = listOf(
                        "crdt_key",
                        "entityType",
                        "operationType",
                        "originDeviceId",
                        "hlcTimestamp",
                        "messageData",
                        "userId"
                    )
                    val missingFields = requiredFields.filter { !firstJson.has(it) }
                    if (missingFields.isNotEmpty()) {
                        Log.w(TAG, "警告: JSON缺少必要字段: $missingFields")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "分析JSON结构时出错: ${e.message}")
                }
            }

            // 记录请求详情
            Log.d(TAG, "构建API请求: POST /sync/messages/$entityType")
            Log.d(TAG, "请求头: X-Device-ID: $deviceId")
            Log.d(TAG, "消息体: ${serializedMessages.size}条JSON消息")

            // 执行API调用
            val response = try {
                Log.d(TAG, "开始执行API调用...")
                val apiResponse = syncApi.uploadMessages(
                    deviceId = deviceId,
                    entityType = entityType,
                    messages = serializedMessages
                )
                Log.d(TAG, "API调用完成，HTTP状态码: ${apiResponse.code()}")
                apiResponse
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "调用syncApi.uploadMessages发生异常: ${e::class.java.simpleName}: ${e.message}",
                    e
                )
                e.printStackTrace()
                throw e
            }

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                Log.d(
                    TAG,
                    "上传响应详情: code=${apiResponse.code}, message=${apiResponse.message}, data=${apiResponse.data}"
                )

                if (apiResponse.code == 200) {
                    Log.d(TAG, "消息上传成功: ${messages.size}条消息已接收，实体类型: $entityType")
                    // 更新消息状态为已同步
                    val messageIds = messages.map { it.id }
                    Log.d(TAG, "正在标记以下消息为已处理: $messageIds")
                    syncMessageDao.markAsProcessed(messageIds.map { it.toString() })
                    Log.d(TAG, "======== 上传同步消息完成: 成功 ========")
                    return messageIds.map { it.toString() } // 返回字符串格式的ID以保持兼容性
                } else {
                    val errorMsg = apiResponse.message
                    Log.e(TAG, "消息上传失败: $errorMsg, 实体类型: $entityType")
                    // 更新消息状态为同步失败
                    val failedMessages = messages.map {
                        Log.d(TAG, "标记消息${it.id}为失败状态，错误: $errorMsg")
                        it.withStatus(SyncConstants.SyncStatus.FAILED, errorMsg)
                    }
                    syncMessageDao.updateAll(failedMessages)
                }
            } else {
                // 更新消息状态为同步失败
                val errorCode = response.code()
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(
                    TAG,
                    "上传失败, HTTP状态码: $errorCode, 错误: $errorBody, 实体类型: $entityType"
                )

                // 提供更多HTTP错误详情
                Log.e(TAG, "HTTP错误详情 - 请求URL: ${response.raw().request.url}")
                Log.e(TAG, "HTTP错误详情 - 请求方法: ${response.raw().request.method}")
                Log.e(TAG, "HTTP错误详情 - 请求头: ${response.raw().request.headers}")

                val failedMessages = messages.map {
                    Log.d(TAG, "标记消息${it.id}为失败状态，HTTP错误: $errorCode")
                    it.withStatus(SyncConstants.SyncStatus.FAILED, "HTTP $errorCode: $errorBody")
                }
                syncMessageDao.updateAll(failedMessages)
            }
        } catch (e: IOException) {
            // 网络错误
            Log.e(
                TAG,
                "网络错误，可能是网络连接问题: ${e::class.java.simpleName}: ${e.message}, 实体类型: $entityType",
                e
            )
            e.printStackTrace()
            val failedMessages = messages.map {
                Log.d(TAG, "标记消息${it.id}为失败状态，网络错误: ${e.message}")
                it.withStatus(SyncConstants.SyncStatus.FAILED, "网络错误: ${e.message}")
            }
            syncMessageDao.updateAll(failedMessages)
        } catch (e: HttpException) {
            // HTTP错误
            Log.e(TAG, "HTTP错误: ${e.code()}, 信息: ${e.message()}, 实体类型: $entityType", e)
            e.printStackTrace()
            val failedMessages = messages.map {
                Log.d(TAG, "标记消息${it.id}为失败状态，HTTP错误: ${e.code()}")
                it.withStatus(SyncConstants.SyncStatus.FAILED, "HTTP ${e.code()}: ${e.message()}")
            }
            syncMessageDao.updateAll(failedMessages)
        } catch (e: Exception) {
            // 其他错误
            Log.e(
                TAG,
                "上传过程中发生未知错误: ${e::class.java.simpleName}: ${e.message}, 实体类型: $entityType",
                e
            )
            e.printStackTrace()
            val failedMessages = messages.map {
                Log.d(TAG, "标记消息${it.id}为失败状态，未知错误: ${e::class.java.simpleName}")
                it.withStatus(
                    SyncConstants.SyncStatus.FAILED,
                    "${e::class.java.simpleName}: ${e.message}"
                )
            }
            syncMessageDao.updateAll(failedMessages)
        }

        Log.d(TAG, "======== 上传同步消息完成: 失败 ========")
        return emptyList()
    }

    override suspend fun downloadAllMessages(): List<SyncMessageDto> {
        return try {
            val deviceId = deviceIdManager.getOrCreateDeviceId()
            val response = syncApi.getAllMessages(deviceId)

            if (response.isSuccessful) {
                // 将ApiSyncMessageDto转换为SyncMessageDto
                val apiMessages = response.body() ?: emptyList()
                apiMessages.mapNotNull { it.toSyncMessageDto() }
            } else {
                Log.e(TAG, "Failed to download all messages: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading all messages", e)
            emptyList()
        }
    }

    override suspend fun downloadMessagesByEntityType(entityType: String): List<SyncMessageDto> {
        return try {
            val deviceId = deviceIdManager.getOrCreateDeviceId()
            val response = syncApi.getMessagesByEntityType(deviceId, entityType)

            if (response.isSuccessful) {
                // 将ApiSyncMessageDto转换为SyncMessageDto
                val apiMessages = response.body() ?: emptyList()
                apiMessages.mapNotNull { it.toSyncMessageDto() }
            } else {
                Log.e(
                    TAG,
                    "Failed to download messages for $entityType: ${response.errorBody()?.string()}"
                )
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading messages for $entityType", e)
            emptyList()
        }
    }

    override suspend fun downloadAllMessagesExcludeOrigin(): List<SyncMessageDto> {
        return try {
            val deviceId = deviceIdManager.getOrCreateDeviceId()
            val response = syncApi.getAllMessagesExcludeOrigin(deviceId)

            if (response.isSuccessful) {
                // 将ApiSyncMessageDto转换为SyncMessageDto
                val apiMessages = response.body() ?: emptyList()
                apiMessages.mapNotNull { it.toSyncMessageDto() }
            } else {
                Log.e(
                    TAG,
                    "Failed to download non-origin messages: ${response.errorBody()?.string()}"
                )
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading non-origin messages", e)
            emptyList()
        }
    }

override suspend fun downloadMessagesByEntityTypeExcludeOrigin(entityType: String): List<SyncMessageDto> {
    Log.d(TAG, "开始下载 $entityType 类型的非本地消息")
    // 只支持这五种实体类型的同步
    val supportedTypes = listOf(
        SyncConstants.EntityType.COURSE.value,
        SyncConstants.EntityType.COURSE_NODE.value,
        SyncConstants.EntityType.TABLE.value,
        SyncConstants.EntityType.ORDINARY_SCHEDULE.value,
        SyncConstants.EntityType.TIME_SLOT.value
    )

    if (entityType !in supportedTypes) {
        Log.d(TAG, "跳过不支持的实体类型: $entityType")
        return emptyList()
    }

    return try {
        Log.d(TAG, "准备下载 $entityType 类型的非本地消息")
        val deviceId = deviceIdManager.getOrCreateDeviceId()
        Log.d(TAG, "获取到设备ID: $deviceId，准备请求服务器获取 $entityType 类型的非本地消息")
        val response = syncApi.getMessagesByEntityTypeExcludeOrigin(deviceId, entityType)

        if (response.isSuccessful) {
            Log.d(TAG, "服务器响应成功，开始解析 $entityType 类型的消息")
            // 解析响应，处理可能的格式错误
            try {
                val apiMessages = response.body() ?: emptyList()
                Log.d(TAG, "获取到 ${apiMessages.size} 条 $entityType 类型的API消息")

                // 处理API消息格式，将ApiSyncMessageDto转换为SyncMessageDto
                val processedMessages = apiMessages.mapNotNull { apiMessage ->
                    try {
                        // 使用ApiSyncMessageDto的转换方法获取SyncMessageDto
                        val syncMessage = apiMessage.toSyncMessageDto()
                        if (syncMessage == null) {
                            Log.w(TAG, "无法从API消息中提取有效的同步消息: $apiMessage")
                        }
                        syncMessage
                    } catch (e: Exception) {
                        Log.e(TAG, "处理API消息时出错: ${e.message}, 消息: $apiMessage", e)
                        null
                    }
                }

                if (processedMessages.size < apiMessages.size) {
                    Log.w(
                        TAG,
                        "过滤掉了 ${apiMessages.size - processedMessages.size} 条格式不正确的消息"
                    )
                } else {
                    Log.d(TAG, "所有API消息均成功转换为SyncMessageDto")
                }

                Log.d(TAG, "最终返回 ${processedMessages.size} 条 $entityType 类型的同步消息")
                processedMessages
            } catch (e: Exception) {
                Log.e(TAG, "解析 $entityType 类型的消息时出错: ${e.message}")
                emptyList()
            }
        } else {
            Log.e(
                TAG,
                "下载 $entityType 类型的非本地消息失败: ${response.errorBody()?.string()}"
            )
            emptyList()
        }
    } catch (e: Exception) {
        Log.e(TAG, "下载 $entityType 类型的非本地消息时发生错误", e)
        emptyList()
    }
}

    override suspend fun markMessagesAsProcessed(ids: List<UUID>) {
        syncMessageDao.markAsProcessed(ids.map { it.toString() })
    }

    /**
     * 将同步消息标记为已同步
     * @param ids 消息ID列表
     */
    override suspend fun markMessagesAsSynced(ids: List<UUID>) {
        syncMessageDao.markAsProcessed(ids.map { it.toString() })
    }

    /**
     * 注册设备与用户的关联
     * @param userId 用户ID
     * @return 注册是否成功
     */
    override suspend fun registerDevice(userId: UUID): Boolean {
        return try {
            val deviceId = deviceIdManager.getOrCreateDeviceId()

            Log.d(TAG, "正在注册设备: deviceId=$deviceId, userId=$userId")

            // 确保用户ID有效
            if (!userId.valid()) {
                Log.e(TAG, "无效的用户ID: $userId")
                return false
            }

            // 确保设备ID有效
            if (deviceId.isBlank()) {
                Log.e(TAG, "无效的设备ID: $deviceId")
                return false
            }

            Log.d(TAG, "发送设备注册请求: deviceId=$deviceId, userId=$userId")

            val response = syncApi.registerDevice(
                deviceId = deviceId,
                deviceRegistration = DeviceRegistrationDto(deviceId, userId.toString())
            )

            val success = response.isSuccessful && response.body() != null

            if (success) {
                // 响应中的lastSyncHlcTimestamp可能为null，首次注册或服务器未维护同步时间戳时
                val lastTimestamp = response.body()?.lastSyncHlcTimestamp ?: 0L
                Log.d(
                    TAG,
                    "设备注册成功: 用户ID=$userId, 设备ID=$deviceId, 最后同步时间戳=$lastTimestamp"
                )
                true
            } else {
                val errorBody = response.errorBody()?.string() ?: "未知错误"
                Log.e(TAG, "设备注册失败: ${response.code()}, $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "设备注册过程中发生异常: ${e.message}", e)
            false
        }
    }

    override fun getAllSyncMessages(): Flow<List<SyncMessageEntity>> {
        return syncMessageDao.getAllMessages()
    }

    override suspend fun syncEntityType(entityType: SyncConstants.EntityType): Boolean {
        try {
            Log.d(TAG, "开始同步实体类型: ${entityType.value}")

            // 下载该类型的最新消息
            val remoteMessages = downloadMessagesByEntityTypeExcludeOrigin(entityType.value)
            Log.d(
                TAG,
                "从服务器下载的消息数量: ${remoteMessages.size}, 实体类型: ${entityType.value}"
            )

            if (remoteMessages.isNotEmpty()) {
                // 保存到本地数据库
                val entities = remoteMessages.map { it.toEntity(SyncConstants.SyncStatus.SYNCED) }
                saveSyncMessages(entities)
                Log.d(TAG, "已保存${entities.size}条远程消息到本地，实体类型: ${entityType.value}")
            }

            // 上传本地待同步消息
            val pendingMessages = getPendingMessagesByType(entityType.value)
            Log.d(
                TAG,
                "待上传的本地消息数量: ${pendingMessages.size}, 实体类型: ${entityType.value}"
            )

            if (pendingMessages.isNotEmpty()) {
                val uploadedIds = uploadMessages(pendingMessages, entityType.value)
                Log.d(
                    TAG,
                    "已上传${uploadedIds.size}/${pendingMessages.size}条本地消息，实体类型: ${entityType.value}"
                )
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "同步实体类型失败: ${entityType.value}", e)
            return false
        }
    }

    override suspend fun syncAll(): Boolean {
        try {
            // 只支持这五种实体类型的同步
            val supportedTypes = listOf(
                SyncConstants.EntityType.COURSE,
                SyncConstants.EntityType.COURSE_NODE,
                SyncConstants.EntityType.TABLE,
                SyncConstants.EntityType.ORDINARY_SCHEDULE,
                SyncConstants.EntityType.TIME_SLOT
            )

            // 获取待同步消息总数
            val pendingCount = syncMessageDao.getPendingMessages().size
            Log.d(TAG, "开始同步支持的实体类型，待同步消息总数: $pendingCount")

            // 同步支持的实体类型
            var allSuccess = true
            var syncedCount = 0

            for (entityType in supportedTypes) {
                val startTime = System.currentTimeMillis()
                val success = syncEntityType(entityType)
                val duration = System.currentTimeMillis() - startTime

                if (success) {
                    syncedCount++
                    Log.d(TAG, "实体类型 ${entityType.value} 同步成功，耗时: ${duration}ms")
                } else {
                    allSuccess = false
                    Log.e(TAG, "实体类型 ${entityType.value} 同步失败，耗时: ${duration}ms")
                }
            }

            Log.d(
                TAG,
                "同步完成，成功: $syncedCount/${supportedTypes.size}，同步状态: ${if (allSuccess) "成功" else "部分失败"}"
            )
            return allSuccess
        } catch (e: Exception) {
            Log.e(TAG, "同步支持的实体类型时发生错误", e)
            return false
        }
    }

    override suspend fun cleanupOldMessages(beforeTime: Long): Int {
        return syncMessageDao.deleteProcessedMessages(beforeTime)
    }

    override suspend fun getTokenFromSession(): String? {
        return try {
            sessionRepository.getUserToken()
        } catch (e: Exception) {
            Log.e(TAG, "获取token失败: ${e.message}")
            null
        }
    }

    override suspend fun getUserIdFromSession(): UUID? {
        return try {
            sessionRepository.currentUserIdFlow.first()
        } catch (e: Exception) {
            Log.e(TAG, "获取用户ID失败: ${e.message}")
            null
        }
    }

    /**
     * 执行完整的数据同步流程
     * 在 IO 线程执行，防止阻塞主线程
     */
    override suspend fun syncData(): Unit = withContext(NonCancellable) {
        try {
            Log.d(TAG, "开始执行完整同步流程")

            // 只支持这五种实体类型的同步
            val supportedTypes = listOf(
                SyncConstants.EntityType.COURSE,
                SyncConstants.EntityType.COURSE_NODE,
                SyncConstants.EntityType.TABLE,
                SyncConstants.EntityType.ORDINARY_SCHEDULE,
                SyncConstants.EntityType.TIME_SLOT
            )

            // 1. 获取用户ID
            val userIdFromFlow = getUserIdFromSession()
            Log.d(TAG, "从Session获取到的原始用户ID: $userIdFromFlow")

            val userId = userIdFromFlow ?: run {
                Log.e(TAG, "获取用户ID失败，无法执行同步")
                return@withContext
            }

            Log.d(TAG, "转换后的用户ID: $userId, 是否有效: ${userId.valid()}")

            // 确保用户ID有效
            if (!userId.valid()) {
                Log.e(TAG, "用户ID无效，跳过设备注册")
                return@withContext
            }

            // 2. 注册设备与用户的关联
            Log.d(TAG, "即将注册设备，用户ID: $userId")
            val deviceRegistered = registerDevice(userId)
            if (!deviceRegistered) {
                Log.e(TAG, "设备注册失败，无法继续同步过程")
                return@withContext
            }
            Log.d(TAG, "设备注册成功，用户ID: $userId")

            // 3. 获取所有待上传的本地消息
            val pendingMessages = getPendingMessagesByUserId(userId)
            Log.d(TAG, "待上传消息总数: ${pendingMessages.size}")

            var uploadedCount = 0
            var entityTypesWithChanges = mutableSetOf<String>()

            // 4. 按实体类型分组上传本地消息，只上传支持的类型
            for (entityType in supportedTypes) {
                val typeMessages = pendingMessages.filter { it.entityType == entityType.value }
                if (typeMessages.isNotEmpty()) {
                    entityTypesWithChanges.add(entityType.value)
                    Log.d(TAG, "为实体类型 ${entityType.value} 上传 ${typeMessages.size} 条消息")

                    try {
                        val uploadedIds = uploadMessages(typeMessages, entityType.value)
                        uploadedCount += uploadedIds.size
                        Log.d(
                            TAG,
                            "实体类型 ${entityType.value} 上传成功 ${uploadedIds.size}/${typeMessages.size} 条消息"
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "上传实体类型 ${entityType.value} 的消息失败: ${e.message}")
                    }
                }
            }

            Log.d(
                TAG,
                "上传阶段完成，共上传 $uploadedCount 条消息，涉及 ${entityTypesWithChanges.size} 种实体类型"
            )

            // 5. 下载非本地来源的消息，只下载支持的类型
            var downloadedCount = 0
            var failedDownloadCount = 0
            var processedMessageCount = 0

            // 重置变更实体类型集合
            entityTypesWithChanges = mutableSetOf()

            // 第一步：先下载所有消息，但不处理它们
            val allDownloadedMessages = mutableMapOf<String, List<SyncMessageDto>>()

            // 只下载支持的实体类型
            for (entityType in supportedTypes) {
                try {
                    val remoteMessages = downloadMessagesByEntityTypeExcludeOrigin(entityType.value)

                    if (remoteMessages.isNotEmpty()) {
                        entityTypesWithChanges.add(entityType.value)
                        Log.d(
                            TAG,
                            "为实体类型 ${entityType.value} 下载了 ${remoteMessages.size} 条消息"
                        )

                        // 将远程消息保存到本地
                        val entities =
                            remoteMessages.map { it.toEntity(SyncConstants.SyncStatus.SYNCED) }
                        saveSyncMessages(entities)
                        downloadedCount += entities.size

                        // 将消息按类型存入映射表
                        allDownloadedMessages[entityType.value] = remoteMessages
                    } else {
                        Log.d(TAG, "实体类型 ${entityType.value} 没有新的远程消息")
                    }
                } catch (e: Exception) {
                    failedDownloadCount++
                    Log.e(TAG, "下载实体类型 ${entityType.value} 的消息失败: ${e.message}", e)
                }
            }

            // 第二步：按照依赖顺序处理实体消息
            // 定义正确的处理顺序：先用户，再表，再课程，最后课程节点
            val processingOrder = listOf(
                "USER",                                    // 首先处理用户
                SyncConstants.EntityType.TABLE.value,         // 然后处理表
                SyncConstants.EntityType.COURSE.value,       // 然后处理课程
                SyncConstants.EntityType.COURSE_NODE.value,  // 最后处理课程节点
                SyncConstants.EntityType.ORDINARY_SCHEDULE.value, // 其他实体
                SyncConstants.EntityType.TIME_SLOT.value     // 其他实体
            )

            Log.d(TAG, "开始按照正确的依赖顺序处理实体消息")

            // 按顺序处理消息
            for (entityType in processingOrder) {
                val messages = allDownloadedMessages[entityType] ?: continue

                Log.d(TAG, "开始处理 ${messages.size} 条 $entityType 消息（按依赖顺序）")

                for (message in messages) {
                    try {
                        // 调用SyncManager处理每条下载的消息
                        Log.d(
                            TAG,
                            "处理消息: 类型=${message.entityType}, 操作=${message.operationType}, CRDT键=${message.id}"
                        )
                        syncManager.processReceivedMessage(message)
                        processedMessageCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "处理消息失败: CRDT键=${message.id}, 错误: ${e.message}", e)
                    }
                }

                Log.d(TAG, "完成处理 $entityType 类型的 ${messages.size} 条消息")

                // 在每种类型处理完成后延迟500ms，确保数据库操作完成
//                delay(500)
            }

            Log.d(
                TAG,
                "下载阶段完成，共下载 $downloadedCount 条消息，失败类型数: $failedDownloadCount"
            )

            // 6. 清理过期消息
            val oneWeekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
            val deletedCount = cleanupOldMessages(oneWeekAgo)
            Log.d(TAG, "清理了 $deletedCount 条过期消息")

            Log.d(
                TAG,
                "数据同步完成，上传: $uploadedCount，下载: $downloadedCount，清理: $deletedCount"
            )
        } catch (e: Exception) {
            Log.e(TAG, "数据同步过程中发生错误: ${e.message}", e)
        }
    }

    override suspend fun <T> getEntityById(crdtKey: String): T? = try {
        // 👉 先按 UUID 解析，便于直接给只接受 UUID 的 DAO
        val uuid = runCatching { UUID.fromString(crdtKey) }.getOrNull()

        // 1. Course
        uuid?.let { database.courseDao().getCourseById(it) }?.also {
            Log.d(TAG, "获取到 Course 实体的类名为: ${it::class.qualifiedName}")
            return it as T
        }

        // 2. CourseNode
        uuid?.let { database.courseNodeDao().getCourseNodeById(it) }?.also {
            Log.d(TAG, "获取到 CourseNode 实体的类名为: ${it::class.qualifiedName}")
            return it as T
        }

        // 3. OrdinarySchedule
        uuid?.let { database.ordinaryScheduleDao().getOrdinaryScheduleById(it) }?.also {
            Log.d(TAG, "获取到 OrdinarySchedule 实体的类名为: ${it::class.qualifiedName}")
            return it as T
        }

        // 4. TimeSlot  🔧 新增
        uuid?.let { database.timeSlotDao().getTimeSlotById(it.toString()).firstOrNull() }?.also {
            Log.d(TAG, "获取到 TimeSlot 实体的类名为: ${it::class.qualifiedName}")
            return it as T
        }

        // 5. Table（DAO 返回 Flow，需要 firstOrNull() 取实体）
        database.tableDao()
            .getTableById(crdtKey)
            .firstOrNull()
            ?.also {
                return it as T
            }

        null  // 都没找到
    } catch (e: Exception) {
        Log.e(TAG, "根据 CRDT 键获取实体失败: ${e.message}", e)
        null
    }

    override suspend fun <T> saveEntity(entity: T): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                when (entity) {
                    is CourseEntity -> {
                        val courseId = database.courseDao().insertCourse(entity)
                        Log.d(TAG, "新增实体：" + entity.toString())
                        Log.d(TAG, "新增课程id：" + courseId)
                        true
                    }

                    is CourseNodeEntity -> {
                        database.courseNodeDao().insertCourseNode(entity)
                        true
                    }

                    is TimeSlotEntity -> {
                        Log.d(TAG, "保存TimeSlotEntity：" + entity.toString())
                        database.timeSlotDao().insertTimeSlot(entity)
                        true
                    }

                    is OrdinaryScheduleEntity -> {
                        database.ordinaryScheduleDao().insertOrdinarySchedule(entity)
                        true
                    }

                    is TableEntity -> {
                        val tableId = entity.id
                            database.tableDao().insertTable(entity)
                        Log.d(TAG, "保存课表成功，课表ID：$tableId")

                        // 如果课表名是"默认课表"，自动设置为默认课表
                        if (entity.tableName == "默认课表") {
                            try {
                                val userId = entity.userId
                                Log.d(
                                    TAG,
                                    "下载的是默认课表，设置课表 $tableId 为用户 $userId 的默认课表"
                                )
                                globalSettingRepository.updateDefaultTableIds(
                                    userId,
                                    listOf(tableId)
                                )
                                Log.i(TAG, "成功设置课表 $tableId 为用户 $userId 的默认课表")
                            } catch (e: Exception) {
                                Log.e(TAG, "设置默认课表时出错：${e.message}", e)
                                // 设置默认课表失败不应影响保存操作的成功状态
                            }
                        }

                        true
                    }

                    else -> {
                        Log.e(TAG, "不支持的实体类型: ${entity?.javaClass?.simpleName}")
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "保存实体失败: ${e.message}", e)
                false
            }
        }
    }
} 
