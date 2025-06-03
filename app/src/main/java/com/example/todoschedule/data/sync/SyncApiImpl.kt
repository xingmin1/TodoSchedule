package com.example.todoschedule.data.sync

import android.util.Log
import com.example.todoschedule.data.remote.api.SyncApi as RemoteSyncApi
import com.example.todoschedule.data.sync.dto.ApiSyncMessageDto
import com.example.todoschedule.data.sync.dto.SyncMessageDto
import com.example.todoschedule.data.sync.dto.SyncMessagesDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 同步API实现类
 */
@Singleton
class SyncApiImpl @Inject constructor(
    private val remoteSyncApi: RemoteSyncApi,
    private val deviceIdManager: DeviceIdManager
) : SyncApi {

    private val TAG = "SyncApiImpl"
    private val json = Json { encodeDefaults = true }

    /**
     * 发送同步消息到服务器
     * @param messages 同步消息列表
     * @param userId 用户ID
     * @return 操作结果
     */
    override suspend fun sendMessages(messages: List<SyncMessageDto>, userId: UUID): SyncResult {
        if (messages.isEmpty()) {
            Log.d(TAG, "没有消息需要发送")
            return SyncResult(
                isSuccess = true,
                message = "No messages to send",
                syncedCount = 0
            )
        }

        try {
            val deviceId = deviceIdManager.getOrCreateDeviceId()
            val entityType = messages.first().entityType // 假设所有消息都是同一类型

            // 将每个消息对象序列化为JSON字符串
            val serializedMessages = messages.map { json.encodeToString(it) }

            Log.d(
                TAG,
                "准备通过API发送${messages.size}条消息，实体类型: $entityType, 设备ID: $deviceId, 用户ID: $userId"
            )
            Log.d(TAG, "第一条消息序列化后: ${serializedMessages.firstOrNull()}")

            val response = remoteSyncApi.uploadMessages(
                deviceId = deviceId,
                entityType = entityType,
                messages = serializedMessages
            )

            Log.d(TAG, "API响应状态码: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.code == 200) {
                    Log.d(TAG, "消息上传成功，服务器接收了${messages.size}条消息")
                    return SyncResult(
                        isSuccess = true,
                        message = "Successfully uploaded ${messages.size} messages: ${apiResponse.message}",
                        syncedCount = messages.size
                    )
                } else {
                    val errorMsg = "服务器错误: ${apiResponse.message}"
                    Log.e(TAG, errorMsg)
                    return SyncResult(
                        isSuccess = false,
                        message = "Server error: ${apiResponse.message}, code: ${apiResponse.code}",
                        syncedCount = 0
                    )
                }
            } else {
                val errorMsg = "API错误: ${
                    response.errorBody()?.string() ?: "未知错误"
                }, 代码: ${response.code()}"
                Log.e(TAG, errorMsg)
                return SyncResult(
                    isSuccess = false,
                    message = "API error: ${response.errorBody()?.string() ?: "Unknown error"}",
                    syncedCount = 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "发送消息时发生异常: ${e::class.java.simpleName}: ${e.message}", e)
            return SyncResult(
                isSuccess = false,
                message = "Exception: ${e.message}",
                syncedCount = 0
            )
        }
    }

    /**
     * 从服务器获取同步消息
     * @param userId 用户ID
     * @return 服务器上的同步消息
     */
    override suspend fun getMessages(userId: UUID): List<SyncMessageDto> {
        return try {
            val deviceId = deviceIdManager.getOrCreateDeviceId()
            Log.d(TAG, "准备从服务器获取消息，设备ID: $deviceId, 用户ID: $userId")

            val response = remoteSyncApi.getAllMessagesExcludeOrigin(deviceId)

            if (response.isSuccessful) {
                val apiMessages = response.body() ?: emptyList()
                Log.d(TAG, "成功从服务器获取了${apiMessages.size}条消息")
                // 将API响应消息转换为应用内使用的SyncMessageDto
                apiMessages.mapNotNull { it.toSyncMessageDto() }
            } else {
                Log.e(
                    TAG,
                    "获取消息失败: ${response.errorBody()?.string()}, 代码: ${response.code()}"
                )
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取消息时发生异常: ${e::class.java.simpleName}: ${e.message}", e)
            emptyList()
        }
    }
} 