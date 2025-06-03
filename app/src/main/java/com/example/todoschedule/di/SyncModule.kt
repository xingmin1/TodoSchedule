package com.example.todoschedule.di

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import com.example.todoschedule.data.database.AppDatabase
import com.example.todoschedule.data.repository.SyncRepository
import com.example.todoschedule.data.repository.SyncRepositoryImpl
import com.example.todoschedule.data.sync.DeviceIdManager
import com.example.todoschedule.data.sync.SyncApi
import com.example.todoschedule.data.sync.SyncManager
import com.example.todoschedule.data.sync.SyncMessageUploader
import com.example.todoschedule.data.sync.SyncResult
import com.example.todoschedule.data.sync.dto.SyncMessageDto
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.SessionRepository
import com.tap.synk.Synk
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.UUID
import javax.inject.Provider
import javax.inject.Singleton
import com.example.todoschedule.data.remote.api.SyncApi as RemoteSyncApi

/**
 * 同步模块依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    /**
     * 提供WorkManager实例
     */
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    /**
     * 提供设备ID管理器
     */
    @Provides
    @Singleton
    fun provideDeviceIdManager(
        @ApplicationContext context: Context
    ): DeviceIdManager {
        return DeviceIdManager(context)
    }

    /**
     * 提供同步仓库
     */
    @Provides
    @Singleton
    fun provideSyncRepository(
        database: AppDatabase,
        remoteSyncApi: RemoteSyncApi,
        deviceIdManager: DeviceIdManager,
        sessionRepository: SessionRepository,
        syncManagerProvider: Provider<SyncManager>,
        globalSettingRepository: GlobalSettingRepository
    ): SyncRepository {
        return SyncRepositoryImpl(
            syncMessageDao = database.syncMessageDao(),
            syncApi = remoteSyncApi,
            deviceIdManager = deviceIdManager,
            database = database,
            sessionRepository = sessionRepository,
            syncManagerProvider = syncManagerProvider,
            globalSettingRepository = globalSettingRepository
        )
    }

    /**
     * 提供同步API
     *
     * 创建匿名实现，实际调用remoteSyncApi发送消息
     */
    @Provides
    @Singleton
    fun provideSyncApi(
        remoteSyncApi: RemoteSyncApi,
        deviceIdManager: DeviceIdManager
    ): SyncApi {
        return object : SyncApi {
            override suspend fun sendMessages(
                messages: List<SyncMessageDto>,
                userId: UUID
            ): SyncResult {
                Log.d(
                    "SyncModule",
                    "准备发送" + messages.size + "条消息到服务器，用户ID: " + userId
                )

                // 实际调用remoteSyncApi发送消息
                val deviceId = deviceIdManager.getOrCreateDeviceId()
                val json = Json { encodeDefaults = true }

                try {
                    // 将每个消息逐个上传到指定实体类型的端点
                    var successCount = 0
                    var failureMessages = mutableListOf<String>()

                    // 按实体类型分组处理
                    val messagesByType = messages.groupBy { it.entityType }

                    for ((entityType, typeMessages) in messagesByType) {
                        val serialized = typeMessages.map {
                            json.encodeToString(
                                serializer<SyncMessageDto>(),
                                it
                            )
                        }

                        Log.d(
                            "SyncModule",
                            "开始上传" + serialized.size + "条" + entityType + "类型的消息，第一条: " + (serialized.firstOrNull()
                                ?.take(100) ?: "") + "..."
                        )

                        val response = remoteSyncApi.uploadMessages(
                            deviceId = deviceId,
                            entityType = entityType,
                            messages = serialized
                        )

                        if (response.isSuccessful && response.body() != null) {
                            val apiResponse = response.body()!!
                            // 检查API响应码
                            if (apiResponse.code == 200) {
                                Log.d(
                                    "SyncModule",
                                    "成功上传 " + serialized.size + " 条 " + entityType + " 类型的消息，响应: " + apiResponse.message
                                )
                                successCount += serialized.size
                            } else {
                                val error = apiResponse.message
                                Log.e(
                                    "SyncModule",
                                    "上传 " + entityType + " 类型的消息失败: " + error + "，响应码：" + apiResponse.code
                                )
                                failureMessages.add("Failed to sync $entityType: $error (code: ${apiResponse.code})")
                            }
                        } else {
                            val error = response.errorBody()?.string() ?: "Unknown error"
                            Log.e(
                                "SyncModule",
                                "上传" + entityType + "类型的消息失败: " + error
                            )
                            failureMessages.add("Failed to sync $entityType: $error")
                        }
                    }

                    return SyncResult(
                        isSuccess = failureMessages.isEmpty(),
                        message = if (failureMessages.isEmpty()) "成功同步" + successCount + "条消息" else failureMessages.joinToString(),
                        syncedCount = successCount
                    )
                } catch (e: Exception) {
                    Log.e("SyncModule", "发送消息过程中发生异常: " + e.message, e)
                    return SyncResult(
                        isSuccess = false,
                        message = "Error: " + e.message,
                        syncedCount = 0
                    )
                }
            }

            override suspend fun getMessages(userId: UUID): List<SyncMessageDto> {
                Log.d("SyncModule", "尝试获取用户ID " + userId + " 的所有消息")
                val deviceId = deviceIdManager.getOrCreateDeviceId()
                try {
                    val response = remoteSyncApi.getAllMessages(deviceId)
                    return if (response.isSuccessful) {
                        val apiMessages = response.body() ?: emptyList()
                        Log.d("SyncModule", "成功获取" + apiMessages.size + "条消息")
                        // 将API消息转换为应用内使用的SyncMessageDto
                        apiMessages.mapNotNull { it.toSyncMessageDto() }
                    } else {
                        Log.e(
                            "SyncModule",
                            "获取消息失败: " + (response.errorBody()?.string() ?: "")
                        )
                        emptyList()
                    }
                } catch (e: Exception) {
                    Log.e("SyncModule", "获取消息过程中发生异常: " + e.message, e)
                    return emptyList()
                }
            }
        }
    }

    /**
     * 提供同步消息上传器
     */
    @Provides
    @Singleton
    fun provideSyncMessageUploader(
        syncApi: RemoteSyncApi,
        database: AppDatabase,
        deviceIdManager: DeviceIdManager
    ): SyncMessageUploader {
        return SyncMessageUploader(
            syncApi = syncApi,
            syncMessageDao = database.syncMessageDao(),
            deviceIdManager = deviceIdManager
        )
    }

//    /**
//     * 提供课程适配器
//     */
//    @Provides
//    @Singleton
//    fun provideCourseAdapter(): CourseEntitySynkAdapter {
//        return CourseEntitySynkAdapter()
//    }
//
//    @Provides
//    @Singleton
//    fun provideTableAdapter(): TableEntitySynkAdapter {
//        return TableEntitySynkAdapter()
//    }
//
//    @Provides
//    @Singleton
//    fun provideCourseNodeAdapter(): CourseNodeEntitySynkAdapter {
//        return CourseNodeEntitySynkAdapter()
//    }
//
//    @Provides
//    @Singleton
//    fun provideOrdinaryScheduleAdapter(): OrdinaryScheduleEntitySynkAdapter {
//        return OrdinaryScheduleEntitySynkAdapter()
//    }

    /**
     * 提供SyncManager实例
     */
    @Provides
    @Singleton
    fun provideSyncManager(
        syncRepository: SyncRepository,
        deviceIdManager: DeviceIdManager,
        synk: Synk,
    ): SyncManager {
        return SyncManager(
            syncRepository,
            deviceIdManager,
            synk,
        )
    }
}
