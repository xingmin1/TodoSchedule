package com.example.todoschedule.core.utils

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 开发工具类，提供开发期间使用的各种工具方法
 * 注意：这些方法只应在开发阶段使用，生产环境不应调用
 */
@Singleton
class DevUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase
) {
    /**
     * 清空数据库和 DataStore<Preference>
     * 警告：这会删除所有本地数据！只在开发阶段使用。
     */
    suspend fun clearDatabaseAndDataStore() = withContext(Dispatchers.IO) {
        // 1. 清空数据库
        try {
            db.clearAllTables()
            // Room 会在 clearAllTables 后自动运行 VACUUM (如果需要)
            // 不再需要手动关闭 db，因为它是 Hilt 管理的单例
        } catch (e: Exception) {
            // 处理可能的错误
            e.printStackTrace()
        }

        // 2. 清空 DataStore
        try {
            context.preferencesDataStoreFile(AppConstants.UserPreferencesName)
                .delete() // 使用与 DataStoreModule 中相同的名称
            // 或者使用 edit { it.clear() } 但删除文件更彻底
            // context.dataStore.edit { it.clear() } // 需要注入 dataStore 实例
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. (可选) 清空默认 SharedPreferences
        /*
        try {
            val sharedPrefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            sharedPrefs.edit().clear().apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        */
    }
} 