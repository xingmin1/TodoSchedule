package com.example.todoschedule.data.repository

import android.util.Log
import com.example.todoschedule.data.database.entity.SyncMessageEntity
import com.example.todoschedule.data.sync.SyncMessageUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SyncRepository的扩展函数
 * 提供增强的同步功能，包括改进的上传方法
 */

private const val TAG = "SyncRepositoryExt"

/**
 * 使用优化的上传器上传消息
 *
 * 该扩展函数替代原有uploadMessages方法，提供更可靠的上传功能：
 * 1. 正确的消息序列化格式，符合API要求
 * 2. 智能重试机制
 * 3. 更好的错误处理
 *
 * @param messages 待上传的消息列表
 * @param entityType 实体类型
 * @param uploader 消息上传器实例
 * @return 上传成功的消息ID列表
 */
suspend fun SyncRepository.uploadMessagesEnhanced(
    messages: List<SyncMessageEntity>,
    entityType: String,
    uploader: SyncMessageUploader
): List<String> = withContext(Dispatchers.IO) {
    Log.d(TAG, "使用增强的上传方法上传${messages.size}条消息，实体类型: $entityType")
    return@withContext uploader.uploadMessages(messages, entityType)
}

/**
 * 同步特定类型的实体（增强版）
 *
 * @param entityType 实体类型
 * @param uploader 消息上传器实例
 * @return 是否同步成功
 */
suspend fun SyncRepository.syncEntityTypeEnhanced(
    entityType: String,
    uploader: SyncMessageUploader
): Boolean = withContext(Dispatchers.IO) {
    try {
        Log.d(TAG, "开始同步实体类型: $entityType (增强版)")

        // 获取待同步的消息
        val pendingMessages = getPendingMessagesByType(entityType)
        if (pendingMessages.isEmpty()) {
            Log.d(TAG, "没有待同步的消息，实体类型: $entityType")
            return@withContext true
        }

        // 使用增强的上传方法
        val syncedIds = uploadMessagesEnhanced(pendingMessages, entityType, uploader)
        val success = syncedIds.isNotEmpty()

        if (success) {
            Log.d(TAG, "同步成功，已同步${syncedIds.size}条消息，实体类型: $entityType")
        } else {
            Log.e(TAG, "同步失败，实体类型: $entityType")
        }

        return@withContext success
    } catch (e: Exception) {
        Log.e(TAG, "同步过程中发生异常: ${e.message}, 实体类型: $entityType", e)
        return@withContext false
    }
}

/**
 * 同步所有类型的实体（增强版）
 *
 * @param uploader 消息上传器实例
 * @return 是否全部同步成功
 */
suspend fun SyncRepository.syncAllEnhanced(
    uploader: SyncMessageUploader
): Boolean = withContext(Dispatchers.IO) {
    var allSuccess = true

    for (entityType in com.example.todoschedule.data.sync.SyncConstants.EntityType.values()) {
        val success = syncEntityTypeEnhanced(entityType.name, uploader)
        if (!success) {
            allSuccess = false
        }
    }

    return@withContext allSuccess
}
