package com.example.todoschedule.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.todoschedule.data.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * 同步工作器
 *
 * 负责执行后台同步任务，支持周期性同步和即时同步
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val syncManager: SyncManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val SYNC_WORK_NAME = "sync_work"
        private const val SYNC_INTERVAL_SECONDS = 10L  // 同步频率固定为10秒

        /**
         * 安排周期性同步任务
         */
        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                SYNC_INTERVAL_SECONDS, TimeUnit.SECONDS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        /**
         * 安排即时同步任务
         */
        fun scheduleImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)

            Log.d(TAG, "Scheduled immediate sync work")
        }
    }

    /**
     * 执行同步任务
     */
    override suspend fun doWork(): Result {
        Log.d(TAG, "开始执行同步任务")

        // 检查网络连接
        if (!isNetworkAvailable()) {
            Log.d(TAG, "无网络连接，稍后重试")
            return Result.retry()
        }

        return try {
            // 验证token是否存在
            val token = syncRepository.getTokenFromSession()
            if (token.isNullOrBlank()) {
                Log.d(TAG, "未找到有效token，跳过同步")
                return Result.failure()
            }

            // 获取当前用户ID
            val userId = syncRepository.getUserIdFromSession() ?: run {
                Log.d(TAG, "无法获取用户ID，跳过同步")
                return Result.failure()
            }

            // 确保设备已注册
            val deviceRegistered = syncRepository.registerDevice(userId)
            if (!deviceRegistered) {
                Log.e(TAG, "设备注册失败，跳过同步")
                return Result.failure()
            }

            // 执行同步操作 - 确保同时上传和下载消息
            Log.d(TAG, "开始执行完整同步流程")
            syncRepository.syncData()
            Log.d(TAG, "同步操作完成")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "同步过程中发生异常: ${e.message}", e)
            Result.retry()
        }
    }

    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
} 