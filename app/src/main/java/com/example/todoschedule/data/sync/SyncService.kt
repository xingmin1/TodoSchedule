package com.example.todoschedule.data.sync

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.repository.SyncRepository
import com.example.todoschedule.domain.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

/**
 * 同步服务
 * 负责在后台执行同步操作
 */
@AndroidEntryPoint
class SyncService : Service() {
    @Inject
    lateinit var syncRepository: SyncRepository

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var sessionRepository: SessionRepository

    private val binder = SyncBinder()
    private var syncJob: Job? = null
    private var timer: Timer? = null
    private var isFirstSync = true

    companion object {
        private const val TAG = "SyncService"
        private const val DEFAULT_USER_ID = 1
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 5000L // 5秒
    }

    inner class SyncBinder : Binder() {
        fun getService(): SyncService = this@SyncService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SyncService created")

        // 验证所有依赖是否已注入
        if (::syncRepository.isInitialized) {
            Log.d(TAG, "SyncRepository successfully injected")
        } else {
            Log.e(TAG, "SyncRepository not injected")
        }

        if (::syncManager.isInitialized) {
            Log.d(TAG, "SyncManager successfully injected")
        } else {
            Log.e(TAG, "SyncManager not injected")
        }

        if (::sessionRepository.isInitialized) {
            Log.d(TAG, "SessionRepository successfully injected")
        } else {
            Log.e(TAG, "SessionRepository not injected")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SyncService started with startId: $startId")

        // 第一次启动时检查用户登录状态，只有在用户已登录且有token时才执行同步
        if (isFirstSync) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 确保依赖已初始化
                    if (!::sessionRepository.isInitialized) {
                        Log.e(TAG, "SessionRepository未初始化，跳过首次同步")
                        return@launch
                    }

                    // 检查用户是否已登录且有token
                    val isLoggedIn = sessionRepository.currentUserIdFlow.first() != null
                    val token = sessionRepository.getUserToken()

                    if (isLoggedIn && !token.isNullOrEmpty()) {
                        Log.d(TAG, "用户已登录且有token，执行首次同步...")

                        // 获取用户ID
                        val userId = sessionRepository.currentUserIdFlow.first()?.toInt() ?: run {
                            Log.e(TAG, "无法获取用户ID，跳过设备注册和同步")
                            return@launch
                        }

                        // 主动尝试注册设备
                        Log.d(TAG, "尝试主动注册设备，用户ID: $userId")
                        val deviceRegistered = syncRepository.registerDevice(userId)

                        if (deviceRegistered) {
                            Log.d(TAG, "设备注册成功，开始同步")
                            startPeriodicSync() // 先启动周期性同步
                            syncNow() // 再执行首次同步
                        } else {
                            Log.e(TAG, "设备注册失败，跳过同步过程")
                        }
                    } else {
                        Log.d(TAG, "用户未登录或没有token，跳过首次同步")
                    }
                    isFirstSync = false
                } catch (e: Exception) {
                    Log.e(TAG, "首次同步失败", e)
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        syncJob?.cancel()
        Log.d(TAG, "SyncService destroyed")
    }

    /**
     * 启动周期性同步
     */
    fun startPeriodicSync() {
        if (timer != null) {
            Log.d(TAG, "周期性同步已启动，跳过重复启动")
            return
        }

        Log.d(
            TAG,
            "启动周期性同步，间隔: ${AppConstants.Sync.SYNC_INTERVAL}ms (${AppConstants.Sync.SYNC_INTERVAL / 1000}秒)"
        )
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // 确保依赖已初始化
                        if (!::sessionRepository.isInitialized) {
                            Log.e(TAG, "SessionRepository未初始化，跳过定时同步")
                            return@launch
                        }

                        // 检查用户是否已登录
                        val isLoggedIn = sessionRepository.currentUserIdFlow.first() != null
                        val token = sessionRepository.getUserToken()

                        if (isLoggedIn && !token.isNullOrEmpty()) {
                            // 获取用户ID
                            val userId =
                                sessionRepository.currentUserIdFlow.first()?.toInt() ?: run {
                                    Log.w(TAG, "无法获取用户ID，跳过同步")
                                    return@launch
                                }

                            // 确保设备ID已注册
                            if (!syncRepository.registerDevice(userId)) {
                                Log.w(TAG, "设备注册失败，跳过同步")
                                return@launch
                            }

                            Log.d(TAG, "用户已登录且设备已注册，执行定时同步...")
                            syncNow()
                        } else {
                            Log.d(TAG, "用户未登录或没有令牌，跳过同步")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "定时同步失败", e)
                    }
                }
            }
        }, AppConstants.Sync.SYNC_INITIAL_DELAY, AppConstants.Sync.SYNC_INTERVAL)

        Log.d(TAG, "周期性同步计划已启动")
    }

    /**
     * 同步现在
     * 使用重试机制确保同步操作更可靠
     *
     * @return 同步是否成功
     */
    suspend fun syncNow(): Boolean = withContext(Dispatchers.IO) {
        var retryCount = 0
        var lastException: Exception? = null

        // 确保依赖已初始化
        if (!::sessionRepository.isInitialized) {
            Log.e(TAG, "SessionRepository未初始化，无法执行同步")
            return@withContext false
        }

        if (!::syncRepository.isInitialized) {
            Log.e(TAG, "SyncRepository未初始化，无法执行同步")
            return@withContext false
        }

        // 首先检查用户是否已登录且有token
        val isLoggedIn = sessionRepository.currentUserIdFlow.first() != null
        val token = sessionRepository.getUserToken()

        if (!isLoggedIn || token.isNullOrEmpty()) {
            Log.d(TAG, "用户未登录或没有令牌，跳过同步操作")
            return@withContext false
        }

        Log.d(TAG, "开始同步操作，用户已登录，有效token: ${token.take(10)}...")

        // 获取当前用户ID
        val userId = sessionRepository.currentUserIdFlow.first()?.toInt() ?: run {
            Log.w(TAG, "无法获取当前用户ID，跳过同步")
            return@withContext false
        }

        // 先注册设备，确保设备ID已被服务器识别
        val deviceRegistered = syncRepository.registerDevice(userId)
        if (!deviceRegistered) {
            Log.w(TAG, "设备注册失败，跳过同步操作")
            return@withContext false
        }
        Log.d(TAG, "设备注册成功，开始执行数据同步")

        while (retryCount < MAX_RETRY_COUNT) {
            try {
                Log.d(TAG, "开始执行同步操作 (尝试 ${retryCount + 1}/${MAX_RETRY_COUNT})")

                // 获取待同步的消息数量
                val pendingMessages = syncRepository.getPendingMessages()
                Log.d(TAG, "待同步消息数量: ${pendingMessages.size}")

                // 使用syncData()方法替代syncAll()，确保同时执行上传和下载
                try {
                    syncRepository.syncData()
                    Log.d(TAG, "完整同步流程执行成功")
                    return@withContext true
                } catch (e: Exception) {
                    Log.e(TAG, "同步过程中发生错误", e)
                    // 继续重试
                    retryCount++
                    if (retryCount < MAX_RETRY_COUNT) {
                        Log.d(TAG, "将在 ${RETRY_DELAY_MS}ms 后重试")
                        delay(RETRY_DELAY_MS)
                    }
                }
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "同步操作失败 (尝试 ${retryCount + 1}/${MAX_RETRY_COUNT})", e)
                retryCount++

                if (retryCount < MAX_RETRY_COUNT) {
                    Log.d(TAG, "将在 ${RETRY_DELAY_MS}ms 后重试")
                    delay(RETRY_DELAY_MS)
                }
            }
        }

        if (lastException != null) {
            Log.e(TAG, "同步操作失败，已达到最大重试次数", lastException)
        } else {
            Log.e(TAG, "同步操作失败，已达到最大重试次数")
        }

        return@withContext false
    }

    /**
     * 停止同步
     */
    fun stopSync() {
        timer?.cancel()
        timer = null
        syncJob?.cancel()
        syncJob = null
        Log.d(TAG, "同步已停止")
    }
} 