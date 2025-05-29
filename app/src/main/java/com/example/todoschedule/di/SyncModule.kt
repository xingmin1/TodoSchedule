package com.example.todoschedule.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.todoschedule.data.database.AppDatabase
import com.example.todoschedule.data.remote.api.SyncApi as RemoteSyncApi
import com.example.todoschedule.data.repository.SyncRepository
import com.example.todoschedule.data.repository.SyncRepositoryImpl
import com.example.todoschedule.data.sync.DeviceIdManager
import com.example.todoschedule.data.sync.dto.ApiSyncMessageDto
import com.example.todoschedule.data.sync.dto.SyncMessageDto
import com.example.todoschedule.data.sync.SyncApi
import com.example.todoschedule.data.sync.SyncMessageUploader
import com.example.todoschedule.data.sync.SyncResult
import com.example.todoschedule.data.sync.adapter.CourseAdapter
import com.example.todoschedule.data.sync.adapter.CourseNodeAdapter
import com.example.todoschedule.data.sync.adapter.OrdinaryScheduleAdapter
import com.example.todoschedule.data.sync.adapter.SynkAdapter
import com.example.todoschedule.data.sync.adapter.SynkAdapterRegistry
import com.example.todoschedule.data.sync.adapter.TableAdapter
import com.example.todoschedule.data.sync.adapter.TimeSlotAdapter
import com.example.todoschedule.data.sync.SyncManager
import com.example.todoschedule.data.sync.CrdtKeyResolver
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton
import javax.inject.Qualifier
import javax.inject.Provider

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
        sessionRepository: com.example.todoschedule.domain.repository.SessionRepository,
        syncManagerProvider: Provider<SyncManager>,
        globalSettingRepository: com.example.todoschedule.domain.repository.GlobalSettingRepository
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
                messages: List<com.example.todoschedule.data.sync.dto.SyncMessageDto>,
                userId: Int
            ): com.example.todoschedule.data.sync.SyncResult {
                android.util.Log.d(
                    "SyncModule",
                    "准备发送" + messages.size + "条消息到服务器，用户ID: " + userId
                )

                // 实际调用remoteSyncApi发送消息
                val deviceId = deviceIdManager.getOrCreateDeviceId()
                val json = kotlinx.serialization.json.Json { encodeDefaults = true }

                try {
                    // 将每个消息逐个上传到指定实体类型的端点
                    var successCount = 0
                    var failureMessages = mutableListOf<String>()

                    // 按实体类型分组处理
                    val messagesByType = messages.groupBy { it.entityType }

                    for ((entityType, typeMessages) in messagesByType) {
                        val serialized = typeMessages.map {
                            json.encodeToString(
                                kotlinx.serialization.serializer<com.example.todoschedule.data.sync.dto.SyncMessageDto>(),
                                it
                            )
                        }

                        android.util.Log.d(
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
                                android.util.Log.d(
                                    "SyncModule",
                                    "成功上传 " + serialized.size + " 条 " + entityType + " 类型的消息，响应: " + apiResponse.message
                                )
                                successCount += serialized.size
                            } else {
                                val error = apiResponse.message
                                android.util.Log.e(
                                    "SyncModule",
                                    "上传 " + entityType + " 类型的消息失败: " + error + "，响应码：" + apiResponse.code
                                )
                                failureMessages.add("Failed to sync $entityType: $error (code: ${apiResponse.code})")
                            }
                        } else {
                            val error = response.errorBody()?.string() ?: "Unknown error"
                            android.util.Log.e(
                                "SyncModule",
                                "上传" + entityType + "类型的消息失败: " + error
                            )
                            failureMessages.add("Failed to sync $entityType: $error")
                        }
                    }

                    return com.example.todoschedule.data.sync.SyncResult(
                        isSuccess = failureMessages.isEmpty(),
                        message = if (failureMessages.isEmpty()) "成功同步" + successCount + "条消息" else failureMessages.joinToString(),
                        syncedCount = successCount
                    )
                } catch (e: Exception) {
                    android.util.Log.e("SyncModule", "发送消息过程中发生异常: " + e.message, e)
                    return com.example.todoschedule.data.sync.SyncResult(
                        isSuccess = false,
                        message = "Error: " + e.message,
                        syncedCount = 0
                    )
                }
            }

            override suspend fun getMessages(userId: Int): List<com.example.todoschedule.data.sync.dto.SyncMessageDto> {
                android.util.Log.d("SyncModule", "尝试获取用户ID " + userId + " 的所有消息")
                val deviceId = deviceIdManager.getOrCreateDeviceId()
                try {
                    val response = remoteSyncApi.getAllMessages(deviceId)
                    return if (response.isSuccessful) {
                        val apiMessages = response.body() ?: emptyList()
                        android.util.Log.d("SyncModule", "成功获取" + apiMessages.size + "条消息")
                        // 将API消息转换为应用内使用的SyncMessageDto
                        apiMessages.mapNotNull { it.toSyncMessageDto() }
                    } else {
                        android.util.Log.e(
                            "SyncModule",
                            "获取消息失败: " + (response.errorBody()?.string() ?: "")
                        )
                        emptyList()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SyncModule", "获取消息过程中发生异常: " + e.message, e)
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

    /**
     * 提供课程适配器
     */
    @Provides
    @Singleton
    fun provideCourseAdapter(): CourseAdapter {
        return CourseAdapter()
    }

    @Provides
    @Singleton
    fun provideTableAdapter(): TableAdapter {
        return TableAdapter()
    }

    @Provides
    @Singleton
    fun provideCourseNodeAdapter(): CourseNodeAdapter {
        return CourseNodeAdapter()
    }

    @Provides
    @Singleton
    fun provideOrdinaryScheduleAdapter(): OrdinaryScheduleAdapter {
        return OrdinaryScheduleAdapter()
    }

    /**
     * 提供Synk适配器注册表
     */
    @Provides
    @Singleton
    fun provideSynkAdapterRegistry(
        courseAdapter: CourseAdapter,
        tableAdapter: TableAdapter,
        courseNodeAdapter: CourseNodeAdapter,
        ordinaryScheduleAdapter: OrdinaryScheduleAdapter,
        timeSlotAdapter: TimeSlotAdapter
    ): SynkAdapterRegistry {
        return SynkAdapterRegistry(
            courseAdapter,
            tableAdapter,
            courseNodeAdapter,
            ordinaryScheduleAdapter,
            timeSlotAdapter
        )
    }

    /**
     * 提供SyncManager实例
     */
    @Provides
    @Singleton
    fun provideSyncManager(
        syncRepository: SyncRepository,
        deviceIdManager: DeviceIdManager,
        synkAdapterRegistry: SynkAdapterRegistry,
        crdtKeyResolver: CrdtKeyResolver
    ): SyncManager {
        return SyncManager(
            syncRepository,
            deviceIdManager,
            synkAdapterRegistry,
            crdtKeyResolver
        )
    }
}
