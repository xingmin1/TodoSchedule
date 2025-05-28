package com.example.todoschedule.data.sync

import com.example.todoschedule.data.sync.dto.SyncMessageDto
import com.example.todoschedule.data.sync.dto.SyncMessagesDto

/**
 * 同步API接口
 * 定义与服务器通信的同步操作
 */
interface SyncApi {
    /**
     * 发送同步消息到服务器
     * @param messages 同步消息列表
     * @param userId 用户ID
     * @return 操作结果
     */
    suspend fun sendMessages(messages: List<SyncMessageDto>, userId: Int): SyncResult

    /**
     * 从服务器获取同步消息
     * @param userId 用户ID
     * @return 服务器上的同步消息
     */
    suspend fun getMessages(userId: Int): List<SyncMessageDto>
}

/**
 * 同步操作结果
 */
data class SyncResult(
    val isSuccess: Boolean,
    val message: String? = null,
    val syncedCount: Int = 0
) 