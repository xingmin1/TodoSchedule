package com.example.todoschedule.data.sync.entity

// import app.cash.sqldelight.Query // 移除未使用的导入
// import com.charlietap.synk.Node // 移除未使用的导入
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.todoschedule.data.sync.SyncConstants
import com.example.todoschedule.data.sync.dto.SyncMessageDto
import com.example.todoschedule.data.sync.dto.TimestampDto
import com.tap.hlc.HybridLogicalClock
import com.tap.hlc.NodeID
import com.tap.hlc.Timestamp
import kotlinx.datetime.Instant
import java.util.UUID

/**
 * 同步实体接口
 *
 * 所有需要在设备间同步的实体都必须实现这个接口。该接口定义了CRDT数据同步所需的
 * 基本属性，包括全局唯一标识符、时间戳和节点信息。
 */
interface SyncEntity {
    /**
     * 实体的本地数据库ID
     *
     * 注意：这个ID只在本地设备上有意义，不应用于跨设备同步
     */
    val id: UUID

    /**
     * 实体的CRDT全局唯一标识符
     *
     * 用于在所有设备间唯一标识该实体，通常使用UUID生成
     */
    val crdtKey: String

    /**
     * 实体的创建时间戳
     *
     * 记录实体首次创建的时间，用于CRDT的因果一致性维护
     */
    val createdAt: Instant

    /**
     * 实体的最后更新时间戳
     *
     * 记录实体最后一次更新的时间，用于冲突解决
     */
    val updatedAt: Instant

    /**
     * 删除标记
     *
     * CRDT系统中不会真正删除数据，而是标记为已删除
     * 这样可以正确处理删除操作与其他操作之间的冲突
     */
    val isDeleted: Boolean

    /**
     * 创建该实体的设备/节点ID
     *
     * 记录首次创建该实体的设备标识符，用于追踪数据来源
     */
    val nodeId: String

    /**
     * 获取该实体对应的实体类型
     *
     * @return 实体类型标识符
     */
    fun getEntityType(): String

    /**
     * 将实体转换为同步消息负载
     *
     * @return 序列化后的实体数据
     */
    fun toSyncPayload(): String
}

/**
 * 同步消息实体
 * 用于存储在本地数据库中的同步消息记录
 */
@Entity(tableName = "sync_messages")
data class SyncMessageEntity(
    @PrimaryKey
    val id: String,                        // 消息唯一ID
    val crdtKey: String,                   // 实体在分布式系统中的唯一标识符
    val entityType: String,                // 实体类型
    val operationType: String,             // 操作类型
    val deviceId: String,                  // 设备ID
    val timestampWallClock: Long,          // 物理时钟时间
    val timestampLogical: Long,            // 逻辑时钟
    val timestampNodeId: String,           // 节点ID
    val payload: String,                   // 消息负载（实体JSON序列化数据）
    val userId: UUID,                       // 用户ID
    val syncStatus: String = SyncConstants.SyncStatus.PENDING.name,  // 同步状态
    val syncedAt: Long = 0                 // 同步时间
) {
    /**
     * 获取混合逻辑时钟时间戳
     *
     * @return 时间戳对象
     */
    fun getTimestamp(): Timestamp {
        return Timestamp(epochMillis = timestampWallClock)
    }

    /**
     * 获取混合逻辑时钟
     *
     * @return 混合逻辑时钟对象
     */
    fun getHybridLogicalClock(): HybridLogicalClock {
        val timestamp = Timestamp(epochMillis = timestampWallClock)
        val nodeId = NodeID(identifier = timestampNodeId)
        return HybridLogicalClock(
            timestamp = timestamp,
            node = nodeId,
            counter = timestampLogical.toInt()
        )
    }

    /**
     * 转换为DTO对象
     */
    fun toDto(): SyncMessageDto {
        return SyncMessageDto(
            crdtKey = crdtKey,
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
} 