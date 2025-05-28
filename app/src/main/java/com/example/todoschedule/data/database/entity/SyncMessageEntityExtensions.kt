package com.example.todoschedule.data.database.entity

import com.example.todoschedule.data.sync.SyncConstants
import com.example.todoschedule.data.sync.dto.SyncMessageDto
import com.example.todoschedule.data.sync.dto.TimestampDto

/**
 * SyncMessageEntity的扩展函数
 */

/**
 * 将同步消息实体转换为DTO对象
 *
 * 注意：实际的SyncMessageEntity类中已经有了toDto方法，
 * 这个扩展函数是多余的，仅作为编译时兼容使用
 */
fun SyncMessageEntity.toDto(): SyncMessageDto {
    return SyncMessageDto(
        crdtKey = this.crdtKey,
        entityType = this.entityType,
        operationType = this.operationType,
        deviceId = this.deviceId,
        timestamp = TimestampDto(
            wallClockTime = this.timestampWallClock,
            logicalTime = this.timestampLogical,
            nodeId = this.timestampNodeId
        ),
        payload = this.payload,
        userId = this.userId
    )
}

/**
 * 更新同步消息状态
 *
 * 注意：实际的SyncMessageEntity类中已经有了withStatus方法，
 * 这个扩展函数是多余的，仅作为编译时兼容使用
 *
 * @param status 新状态
 * @param message 可选的状态消息
 * @return 更新状态后的新实体
 */
fun SyncMessageEntity.withStatus(
    status: SyncConstants.SyncStatus,
    message: String? = null
): SyncMessageEntity {
    return this.copy(
        syncStatus = status.name,
        syncError = message ?: this.syncError
    )
}
