package com.example.todoschedule

import android.app.Application
import android.util.Log
import com.example.todoschedule.core.utils.DevUtils
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 应用类 */
@HiltAndroidApp
class TodoScheduleApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 开发模式设置
    companion object {
        // 开发模式开关：是否每次启动都清空数据库（仅在开发阶段设为true）
        private const val CLEAR_DATABASE_ON_START = true // 默认关闭清空数据库，避免数据丢失
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
                Log.d("TodoScheduleApplication", "清空数据库...")
                DevUtils.clearDatabase(this@TodoScheduleApplication)
            }
        }
    }
}
