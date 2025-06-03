package com.example.todoschedule.data.sync.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


/**
 * 同步消息传输对象
 * 用于在客户端和服务器之间传输同步消息
 *
 * 注意：异步消息不需要全局唯一标识符，因为它们只是传输载体而非实体本身
 */
@Parcelize
@Serializable
data class SyncMessageDto(
    // 实体在分布式系统中的唯一标识符 (CRDT中的全局唯一ID)
    @SerialName("id")
    val id: String,

    // 实体类型
    val entityType: String,

    // 操作类型
    val operationType: String,

    // 设备ID
    @SerialName("originDeviceId")
    val deviceId: String,

    // 混合逻辑时钟时间戳
    @SerialName("hlcTimestamp")
    @Serializable(with = TimestampDtoSerializer::class)
    val timestamp: TimestampDto,

    // 消息负载（实体JSON序列化数据）
    @SerialName("messageData")
    val payload: String,

    // 用户ID
    val userId: String
) : Parcelable

/**
 * 自定义序列化器，用于将TimestampDto序列化为Long值
 *
 * 根据API文档要求，hlcTimestamp需要序列化为单一的Long值
 */
object TimestampDtoSerializer : KSerializer<TimestampDto> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("TimestampDto", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: TimestampDto) {
        // 仅使用物理时钟部分，与API文档一致
        encoder.encodeLong(value.wallClockTime)
    }

    override fun deserialize(decoder: Decoder): TimestampDto {
        val timestamp = decoder.decodeLong()
        return TimestampDto(
            wallClockTime = timestamp,
            logicalTime = 0,
            nodeId = ""
        )
    }
}

/**
 * 同步设备注册请求DTO
 */
@Serializable
data class DeviceRegistrationDto(
    val deviceId: String,      // 设备ID
    val userId: String            // 用户ID
)

/**
 * 同步设备注册响应DTO
 * 与服务器响应格式一致
 */
@Serializable
data class DeviceRegistrationResponseDto(
    val id: String,                    // 设备ID
    val userId: String,                   // 用户ID
    val name: String?,                 // 设备名称
    val lastSyncHlcTimestamp: Long?,   // 最后同步的HLC时间戳，首次注册为null
    val createdAt: String?,             // 创建时间
    val updatedAt: String              // 更新时间
)

/**
 * 混合逻辑时钟时间戳传输对象
 *
 * HybridLogicalClock(HLC)作为CRDT中的关键组件，用于为分布式系统中的事件提供全序关系
 * 它结合了物理时钟(wallClockTime)和逻辑时钟(counter)的特性
 */
@Parcelize
@Serializable
data class TimestampDto(
    // 物理时钟时间（毫秒），用于与真实世界时间保持粗略同步
    val wallClockTime: Long,

    // 逻辑计数器，用于保证同一物理时间内事件的顺序
    val logicalTime: Long,

    // 节点标识符，用于区分不同设备
    val nodeId: String
) : Parcelable {
    /**
     * 转换为时间戳值
     *
     * 注意: 在CRDT系统中，这个转换至关重要，因为时间戳用于冲突解决
     */
    fun toTimestamp(): Long {
        return this.wallClockTime
    }

    companion object {
        /**
         * 从Synk的Timestamp创建TimestampDto
         */
        fun fromSynkTimestamp(timestamp: com.tap.hlc.Timestamp): TimestampDto {
            return TimestampDto(
                wallClockTime = timestamp.epochMillis,
                logicalTime = 0, // 需要从HybridLogicalClock获取counter
                nodeId = "" // 需要从HybridLogicalClock获取nodeId
            )
        }

        /**
         * 从HybridLogicalClock创建完整的TimestampDto
         * 保留全部HLC信息，用于本地冲突解决
         */
        fun fromHlc(hlc: com.tap.hlc.HybridLogicalClock): TimestampDto {
            return TimestampDto(
                wallClockTime = hlc.timestamp.epochMillis,
                logicalTime = hlc.counter.toLong(),
                nodeId = hlc.node.identifier
            )
        }
    }
}

/**
 * 同步消息集合传输对象
 * 用于批量传输多个同步消息
 */
@Serializable
data class SyncMessagesDto(
    val messages: List<SyncMessageDto>
)

/**
 * 上传同步消息响应DTO
 */
/**
 * 上传同步消息响应
 * 已废弃，请使用ApiResponse<UploadSyncResult>
 */
@Serializable
data class UploadSyncResponseDto(
    val success: Boolean,
    val messagesReceived: Int,
    val errors: List<String>? = null
)

/**
 * 上传同步消息响应结果
 * 服务器接收同步消息后的返回数据
 */
@Serializable
data class UploadSyncResult(
    // 根据API文档，此处data字段通常为null
    // 但为了兼容性预留可能的返回字段
    val messagesReceived: Int = 0,
    val timestamp: Long? = null
) 