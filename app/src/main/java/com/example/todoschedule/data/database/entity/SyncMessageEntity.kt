package com.example.todoschedule.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoschedule.data.sync.SyncConstants
import com.example.todoschedule.data.sync.dto.SyncMessageDto
import com.example.todoschedule.data.sync.dto.TimestampDto
import com.tap.hlc.HybridLogicalClock
import com.tap.hlc.NodeID
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * 同步消息实体
 *
 * 用于保存本地产生的和从服务器收到的CRDT同步消息。消息包含必要的元数据
 * 以支持CRDT的冲突解决和因果一致性维护。
 *
 * 在CRDT系统中，消息是关键的数据结构，具有可交换性(Commutative)、
 * 可结合性(Associative)和幂等性(Idempotent)的特性。
 *
 * 注意：同步消息表使用自增ID作为主键，因为消息本身不是CRDT实体，
 * 而只是用于传输CRDT实体数据的载体。异步消息不需要全局唯一标识符。
 */
@Entity(
    tableName = "sync_message",
    indices = [
        Index(value = ["sync_status"]),
        Index(value = ["entity_type"]),
        Index(value = ["crdt_key"])
    ]
)
data class SyncMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: UUID = UUID.randomUUID(),

    // 实体类型，用于区分不同种类的数据
    @ColumnInfo(name = "entity_type") val entityType: String,

    // 操作类型（添加、更新、删除）
    @ColumnInfo(name = "operation_type") val operationType: String,

    // 消息创建时间，使用系统时钟
    @ColumnInfo(name = "created_at") val createdAt: Long = Clock.System.now().toEpochMilliseconds(),

    // 设备ID，标识消息的来源设备
    @ColumnInfo(name = "device_id") val deviceId: String,

    // HLC物理时钟时间，用于冲突解决
    @ColumnInfo(name = "timestamp_wall_clock") val timestampWallClock: Long,

    // HLC逻辑时钟，解决同一物理时间下的事件顺序
    @ColumnInfo(name = "timestamp_logical") val timestampLogical: Long,

    // HLC节点ID，标识时间戳的来源
    @ColumnInfo(name = "timestamp_node_id") val timestampNodeId: String,

    // 消息负载，包含序列化后的实体数据
    @ColumnInfo(name = "payload") val payload: String,

    // 用户ID，标识数据所属的用户
    @ColumnInfo(name = "user_id") val userId: UUID,

    // 同步状态（待同步、同步中、已同步、同步失败）
    @ColumnInfo(name = "sync_status") val syncStatus: String = SyncConstants.SyncStatus.PENDING.name,

    // 最后一次同步尝试时间
    @ColumnInfo(name = "last_sync_attempt") val lastSyncAttempt: Long? = null,

    // 同步错误信息
    @ColumnInfo(name = "sync_error") val syncError: String? = null
) {
    /**
     * 转换为DTO对象用于网络传输
     *
     * @return 同步消息DTO
     */
    fun toDto(): SyncMessageDto {
        return SyncMessageDto(
            id = id.toString(),
            entityType = entityType,
            operationType = operationType,
            deviceId = deviceId,
            timestamp = TimestampDto(
                wallClockTime = timestampWallClock,
                logicalTime = timestampLogical,
                nodeId = timestampNodeId
            ),
            payload = payload,
            userId = userId.toString()
        )
    }

    /**
     * 获取混合逻辑时钟时间戳
     *
     * @return 时间戳对象
     */
    fun getTimestamp(): com.tap.hlc.Timestamp {
        return com.tap.hlc.Timestamp(epochMillis = timestampWallClock)
    }

    /**
     * 获取混合逻辑时钟
     *
     * @return 混合逻辑时钟对象
     */
    fun getHybridLogicalClock(): HybridLogicalClock {
        val timestamp = com.tap.hlc.Timestamp(epochMillis = timestampWallClock)
        val nodeId = NodeID(identifier = timestampNodeId)
        return HybridLogicalClock(
            timestamp = timestamp,
            node = nodeId,
            counter = timestampLogical.toInt()
        )
    }

    /**
     * 更新同步状态
     *
     * @param status 新的同步状态
     * @param error 错误信息（如果有）
     * @return 更新状态后的消息实体
     */
    fun withStatus(status: SyncConstants.SyncStatus, error: String? = null): SyncMessageEntity {
        return this.copy(
            id = id,
            syncStatus = status.name,
            lastSyncAttempt = Clock.System.now().toEpochMilliseconds(),
            syncError = error
        )
    }

    /**
     * 检查此消息是否比另一个消息新
     *
     * 根据HLC时间戳规则比较两个消息的时间顺序
     *
     * @param other 要比较的另一个消息
     * @return 如果此消息较新则返回true
     */
    fun isNewerThan(other: SyncMessageEntity): Boolean {
        val thisHLC = this.getHybridLogicalClock()
        val otherHLC = other.getHybridLogicalClock()
        return thisHLC > otherHLC
    }
}

/**
 * 从DTO转换为实体
 *
 * @param status 同步状态
 * @return 同步消息实体
 */
fun SyncMessageDto.toEntity(
    status: SyncConstants.SyncStatus = SyncConstants.SyncStatus.PENDING
): SyncMessageEntity {
    return SyncMessageEntity(
        entityType = entityType,
        operationType = operationType,
        deviceId = deviceId,
        timestampWallClock = timestamp.wallClockTime,
        timestampLogical = timestamp.logicalTime,
        timestampNodeId = timestamp.nodeId,
        payload = payload,
        userId = userId.let { UUID.fromString(it) },
        syncStatus = status.name
    )
} 