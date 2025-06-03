package com.example.todoschedule.data.sync.dto

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.json.JSONObject

/**
 * API同步消息传输对象
 * 用于解析服务器返回的同步消息格式
 */
@JsonClass(generateAdapter = true)
data class ApiSyncMessageDto(
    val id: Long,
    val userId: UUID,
    val entityType: String,
    val crdtKey: String,
    // 服务端返回的嵌套JSON字符串
    val messageData: String,
    val hlcTimestamp: Long,
    val originDeviceId: String,
    val createdAt: String
) {
    /**
     * 将API格式的消息转换为应用内使用的SyncMessageDto
     * 注意：这个方法需要自行解析messageData中的嵌套JSON
     */
    fun toSyncMessageDto(): SyncMessageDto? {
        return try {
            // 使用JSONObject解析嵌套的JSON字符串
            val jsonObj = JSONObject(messageData)

            // 提取operationType值
            if (!jsonObj.has("operationType")) {
                Log.w("ApiSyncMessageDto", "消息缺少operationType字段: $messageData")
                return null
            }

            val operationType = jsonObj.getString("operationType")

            // 获取嵌套的messageData数据
            val innerPayload = if (jsonObj.has("messageData")) {
                jsonObj.getString("messageData")
            } else {
                Log.w("ApiSyncMessageDto", "消息缺少messageData字段: $messageData")
                ""
            }

            // 构建应用内使用的SyncMessageDto
            SyncMessageDto(
                crdtKey = crdtKey,
                entityType = entityType,
                operationType = operationType,
                deviceId = originDeviceId,
                timestamp = TimestampDto(hlcTimestamp, 0, ""),  // 使用简化的时间戳
                payload = innerPayload,
                userId = userId
            )
        } catch (e: Exception) {
            Log.e("ApiSyncMessageDto", "解析消息时出错: ${e.message}", e)
            null
        }
    }

}

/**
 * API同步消息列表响应DTO
 */
@JsonClass(generateAdapter = true)
data class ApiSyncMessagesResponseDto(
    val messages: List<ApiSyncMessageDto>
)
