package com.example.todoschedule.data.database

import android.util.Log
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** 数据库初始化器 负责在应用首次启动时初始化必要的数据 */
@Singleton
class DatabaseInitializer
@Inject
constructor(
    private val userRepository: UserRepository,
    private val globalSettingRepository: GlobalSettingRepository,
    private val tableRepository: TableRepository
) {
    /** 初始化状态 */
    private val _isInitialized = MutableStateFlow(false)

    /** 初始化状态的可观察流 */
    val isInitialized: StateFlow<Boolean> = _isInitialized

    /** 初始化数据库 如果没有默认用户和课表，则创建 */
    suspend fun initializeDatabase() =
        withContext(Dispatchers.IO) {
            try {
                if (_isInitialized.value) {
                    Log.d("DatabaseInitializer", "数据库已初始化，跳过初始化过程")
                    return@withContext
                }

                Log.d("DatabaseInitializer", "开始数据库初始化")

                // 初始化用户
                val userId = userRepository.initDefaultUserIfNeeded()
                Log.d("DatabaseInitializer", "初始化用户完成，ID: $userId")

                // 初始化全局设置
                val settingId = globalSettingRepository.initDefaultSettingIfNeeded(userId)
                Log.d("DatabaseInitializer", "初始化全局设置完成，ID: $settingId")

                // TODO: 默认课表应该由用户创建或导入课表时创建，而不是在这里创建
                // 检查是否需要创建默认课表
                val tables = tableRepository.fetchTablesByUserId(userId)
                val defaultTableExists = tables.isNotEmpty()
                Log.d("DatabaseInitializer", "用户现有课表数量: ${tables.size}")

                if (!defaultTableExists) {
                    // 创建默认课表
                    val defaultTable =
                        Table(
                            userId = userId,
                            tableName = AppConstants.Database.DEFAULT_TABLE_NAME,
                            startDate = AppConstants.Database.DEFAULT_TABLE_START_DATE,
                        )

                    val tableId = tableRepository.addTable(defaultTable).toInt()
                    Log.d("DatabaseInitializer", "创建默认课表完成，ID: $tableId")

                    // 更新全局设置中的默认课表ID
                    globalSettingRepository.updateDefaultTableIds(userId, listOf(tableId))
                    Log.d("DatabaseInitializer", "更新默认课表ID完成")
                } else {
                    Log.d("DatabaseInitializer", "已存在默认课表，无需创建")
                }

                // 初始化完成，更新状态
                _isInitialized.value = true
                Log.d("DatabaseInitializer", "数据库初始化完成")
            } catch (e: Exception) {
                // 即使出错也标记为已初始化，避免卡死
                Log.e(
                    "DatabaseInitializer",
                    "数据库初始化失败: ${e.message}",
                    e
                )
                _isInitialized.value = true
                throw e
            }
        }
}
