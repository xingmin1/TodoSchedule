package com.example.todoschedule.core.utils

import android.content.Context
import androidx.room.Room
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 开发工具类，提供开发期间使用的各种工具方法
 * 注意：这些方法只应在开发阶段使用，生产环境不应调用
 */
object DevUtils {
    /**
     * 清空数据库
     * 警告：这会删除所有数据！只在开发阶段使用。
     */
    suspend fun clearDatabase(context: Context) = withContext(Dispatchers.IO) {
        // 获取数据库实例
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppConstants.Database.DB_NAME,
        ).build()

        try {
            // 清空所有表
            db.clearAllTables()
        } finally {
            // 确保关闭数据库
            db.close()
        }
    }
} 