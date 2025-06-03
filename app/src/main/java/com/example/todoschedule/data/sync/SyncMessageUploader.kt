package com.example.todoschedule.data.sync

import android.util.Log
import com.example.todoschedule.core.extensions.toStringList
import com.example.todoschedule.data.database.dao.SyncMessageDao
import com.example.todoschedule.data.database.entity.SyncMessageEntity
import com.example.todoschedule.data.remote.api.SyncApi
import com.example.todoschedule.util.NetworkUtils
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 同步消息上传器
 *
 * 专门负责处理消息上传逻辑，包括重试机制和错误处理，
 * 确保消息格式符合API要求。
 */
@Singleton
class SyncMessageUploader @Inject constructor(
    private val syncApi: SyncApi,
    private val syncMessageDao: SyncMessageDao,
    private val deviceIdManager: DeviceIdManager
) {
    private val TAG = "SyncMessageUploader"
    private val json = Json { encodeDefaults = true }

    /**
     * 上传同步消息到服务器
     *
     * @param messages 待上传的消息列表
     * @param entityType 实体类型
     * @return 上传成功的消息ID列表
     */
    suspend fun uploadMessages(
        messages: List<SyncMessageEntity>,
        entityType: String
    ): List<String> {
        if (messages.isEmpty()) {
            Log.d(TAG, "没有待上传的消息，entityType: $entityType")
            return emptyList()
        }

        Log.d(TAG, "=== 同步上传开始 [entityType=$entityType] [消息数量=${messages.size}] ===")

        try {
            // 使用NetworkUtils添加重试逻辑
            return NetworkUtils.withRetry(
                maxRetries = 3,
                tag = TAG
            ) {
                // 获取设备ID并记录日志
                val deviceId = deviceIdManager.getOrCreateDeviceId()
                Log.d(TAG, "设备ID: $deviceId")

                // 打印每条原始消息的关键信息
                messages.forEachIndexed { index, entity ->
                    Log.d(
                        TAG,
                        "消息[$index] - ID: ${entity.id}, 类型: ${entity.entityType}, 操作: ${entity.operationType}, 时间戳: ${entity.timestampWallClock}"
                    )
                }

                // 转换为DTO
                val messageDtos = try {
                    messages.map { it.toDto() }
                } catch (e: Exception) {
                    Log.e(TAG, "消息转换为DTO失败: ${e.message}", e)
                    throw e
                }

                // 检查DTO转换是否成功
                messageDtos.forEachIndexed { index, dto ->
                    Log.d(
                        TAG,
                        "DTO[$index] - 类型: ${dto.entityType}, 操作: ${dto.operationType}, 节点: ${dto.timestamp.nodeId}, 逻辑时间: ${dto.timestamp.logicalTime}, 墙钟时间: ${dto.timestamp.wallClockTime}"
                    )
                }

                // 将DTO对象转换为符合API要求的JSON字符串
                val serializedMessages = try {
                    messageDtos.map { json.encodeToString(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "序列化消息失败: ${e.message}", e)
                    throw e
                }

                Log.d(
                    TAG,
                    "准备上传${messages.size}条同步消息，实体类型: $entityType，设备ID: $deviceId"
                )
                if (serializedMessages.isNotEmpty()) {
                    val firstMessage = serializedMessages.first()
                    Log.d(TAG, "第一条消息序列化后: ${firstMessage.take(100)}...")
                    Log.d(TAG, "序列化消息总长度: ${firstMessage.length} 字符")
                }

                // 记录API调用信息
                Log.d(
                    TAG,
                    "调用API: POST /sync/messages/$entityType, 设备ID: $deviceId, 消息数: ${serializedMessages.size}"
                )

                // 调用API
                val response = try {
                    syncApi.uploadMessages(
                        deviceId = deviceId,
                        entityType = entityType,
                        messages = serializedMessages // 发送JSON字符串数组
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "API调用异常: ${e::class.java.simpleName}: ${e.message}", e)
                    throw e
                }

                Log.d(TAG, "HTTP响应状态码: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    Log.d(
                        TAG,
                        "上传响应详情: code=${apiResponse.code}, message=${apiResponse.message}, data=${apiResponse.data}"
                    )

                    if (apiResponse.code == 200) {
                        Log.d(
                            TAG,
                            "消息上传成功: ${messages.size}条消息已接收，实体类型: $entityType"
                        )
                        // 更新消息状态为已同步
                        val messageIds = messages.map { it.id }
                        Log.d(TAG, "标记以下消息ID为已处理: $messageIds")
                        syncMessageDao.markAsProcessed(messageIds.toStringList())
                        Log.d(
                            TAG,
                            "=== 同步上传完成 [状态=成功] [entityType=$entityType] [接收=${messages.size}] ==="
                        )
                        return@withRetry messageIds.map { it.toString() } // 返回字符串格式的ID以保持兼容性
                    } else {
                        // 服务器接收了请求但报告失败
                        val errorMsg = apiResponse.message
                        Log.e(TAG, "消息上传失败: $errorMsg, 实体类型: $entityType")

                        // 更新消息状态为同步失败
                        val failedMessages = messages.map {
                            Log.d(TAG, "标记消息ID ${it.id} 为失败状态, 错误: $errorMsg")
                            it.withStatus(SyncConstants.SyncStatus.FAILED, errorMsg)
                        }
                        syncMessageDao.updateAll(failedMessages)

                        // 如果是可重试的错误，让withRetry机制处理
                        if (NetworkUtils.isRetryableError(errorMsg)) {
                            Log.d(TAG, "检测到可重试的错误，将触发重试机制")
                            throw RuntimeException("可重试的服务器错误: $errorMsg")
                        }

                        Log.d(
                            TAG,
                            "=== 同步上传完成 [状态=失败] [entityType=$entityType] [错误=$errorMsg] ==="
                        )
                        emptyList<String>()
                    }
                } else {
                    // HTTP错误
                    val errorCode = response.code()
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(
                        TAG,
                        "上传失败, HTTP状态码: $errorCode, 错误: $errorBody, 实体类型: $entityType"
                    )

                    // 更新消息状态为同步失败
                    val failedMessages = messages.map {
                        Log.d(TAG, "标记消息ID ${it.id} 为失败状态, HTTP错误: $errorCode")
                        it.withStatus(
                            SyncConstants.SyncStatus.FAILED,
                            "HTTP $errorCode: $errorBody"
                        )
                    }
                    syncMessageDao.updateAll(failedMessages)

                    // 如果是可重试的HTTP状态码，让withRetry机制处理
                    if (NetworkUtils.isRetryableHttpCode(errorCode)) {
                        Log.d(TAG, "检测到可重试的HTTP状态码: $errorCode，将触发重试机制")
                        throw RuntimeException("可重试的HTTP错误: $errorCode")
                    }

                    Log.d(
                        TAG,
                        "=== 同步上传完成 [状态=失败] [entityType=$entityType] [HTTP=$errorCode] ==="
                    )
                    emptyList<String>()
                }
            }
        } catch (e: Exception) {
            // 如果所有重试都失败了，更新消息状态为同步失败
            Log.e(TAG, "所有重试都失败了: ${e.message}, 实体类型: $entityType", e)
            val failedMessages = messages.map {
                Log.d(TAG, "标记消息ID ${it.id} 为最终失败状态，所有重试均失败")
                it.withStatus(SyncConstants.SyncStatus.FAILED, "所有重试失败: ${e.message}")
            }
            syncMessageDao.updateAll(failedMessages)
            Log.d(
                TAG,
                "=== 同步上传完成 [状态=严重失败] [entityType=$entityType] [错误=${e.message}] ==="
            )
            return emptyList()
        }
    }
}
