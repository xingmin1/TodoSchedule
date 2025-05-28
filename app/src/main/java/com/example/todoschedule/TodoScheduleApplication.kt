package com.example.todoschedule

import android.app.Application
import android.content.Intent
import android.util.Log
import com.example.todoschedule.core.utils.DevUtils
import com.example.todoschedule.data.sync.SyncManager
import com.example.todoschedule.data.sync.SyncService
import com.example.todoschedule.domain.repository.SessionRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 应用类 */
@HiltAndroidApp
class TodoScheduleApplication : Application() {

    // 注入 DevUtils
    @Inject
    lateinit var devUtils: DevUtils

    // 注入同步管理器
    @Inject
    lateinit var syncManager: SyncManager

    // 注入会话仓库
    @Inject
    lateinit var sessionRepository: SessionRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 开发模式设置
    companion object {
        private const val TAG = "TodoApp"
        // 开发模式开关：是否每次启动都清空数据库（仅在开发阶段设为true）
        private const val CLEAR_DATABASE_ON_START = false  // 默认关闭清空数据库，避免数据丢失
    }

    override fun onCreate() {
        super.onCreate()

        // 注意：Application的onCreate在整个应用启动过程中最先被调用
        // 这里的操作发生在MainActivity的onCreate之前
        applicationScope.launch(Dispatchers.IO) {
            // 仅在开发模式下清空数据库
            // 第一步：如果开启了开发模式，首先清空数据库
            // 这确保了数据库初始化前先删除所有数据
            if (CLEAR_DATABASE_ON_START) {
                Log.d(TAG, "清空数据库和 DataStore...")
                devUtils.clearDatabaseAndDataStore()
            }

            // 初始化同步管理器
            try {
                syncManager.initialize(applicationScope)
                Log.d(TAG, "同步管理器初始化完成")

                // 启动同步服务
                startSyncService()
            } catch (e: Exception) {
                Log.e(TAG, "初始化同步管理器失败", e)
            }
        }
    }

    /**
     * 启动同步服务
     */
    private fun startSyncService() {
        try {
            val serviceIntent = Intent(this, SyncService::class.java)
            startService(serviceIntent)
            Log.d(TAG, "同步服务已启动")

            // 在应用启动后延迟检查同步状态
            applicationScope.launch {
                try {
                    // 延迟5秒，确保应用稳定运行
                    kotlinx.coroutines.delay(5000)

                    // 检查用户是否已登录
                    if (::sessionRepository.isInitialized) {
                        val userId = sessionRepository.currentUserIdFlow.first()

                        if (userId != null && userId > 0) {
                            Log.d(TAG, "应用启动时检测到已登录用户: $userId，主动触发同步")

                            // 先尝试注册设备
                            val syncRepo = syncManager.getSyncRepository()
                            val deviceRegistered = syncRepo.registerDevice(userId.toInt())

                            if (deviceRegistered) {
                                Log.d(TAG, "设备注册成功，触发同步")
                                syncManager.syncNow(ignoreExceptions = true)
                            } else {
                                Log.e(TAG, "设备注册失败，跳过同步")
                            }
                        } else {
                            Log.d(TAG, "应用启动时未检测到登录用户，跳过同步")
                        }
                    } else {
                        Log.e(TAG, "会话仓库未初始化，无法检查用户登录状态")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "应用启动后检查同步状态失败", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动同步服务失败", e)
        }
    }
}
