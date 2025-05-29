package com.example.todoschedule.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.todoschedule.data.repository.SyncRepository
import com.example.todoschedule.data.sync.SyncService
import com.example.todoschedule.data.sync.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncRepository: SyncRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ... 现有代码 ...

        // 初始化同步服务
        initSyncService()

        // ... 现有代码 ...
    }

    /**
     * 初始化同步服务
     */
    private fun initSyncService() {
        lifecycleScope.launch {
            try {
                // 检查用户是否已登录且有token
                val token = syncRepository.getTokenFromSession()
                if (token.isNullOrEmpty()) {
                    Log.d(TAG, "用户未登录或没有token，不启动同步服务")
                    return@launch
                }

                // 获取当前用户ID
                val userId = syncRepository.getUserIdFromSession() ?: run {
                    Log.d(TAG, "无法获取用户ID，不启动同步服务")
                    return@launch
                }

                Log.d(TAG, "用户已登录(ID: $userId)且有token，启动同步服务")

                // 确保设备ID已成功注册
                val deviceRegistered = syncRepository.registerDevice(userId.toInt())
                if (!deviceRegistered) {
                    Log.d(TAG, "设备注册失败，不启动同步服务")
                    return@launch
                }

                Log.d(TAG, "设备注册成功，启动同步服务")

                // 启动SyncService服务
                val intent = Intent(this@MainActivity, SyncService::class.java)
                startService(intent)

                // 同时安排WorkManager任务作为备份同步机制
                val tokenData = androidx.work.Data.Builder()
                    .putInt("userId", userId.toInt())
                    .putString("token", token)
                    .build()

                SyncWorker.schedulePeriodic(this@MainActivity)

                Log.d(TAG, "同步系统初始化完成")
            } catch (e: Exception) {
                Log.e(TAG, "同步服务初始化失败", e)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val DEFAULT_USER_ID = 1
    }
} 