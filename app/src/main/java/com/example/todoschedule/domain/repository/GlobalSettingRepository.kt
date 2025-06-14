package com.example.todoschedule.domain.repository

import com.example.todoschedule.domain.model.GlobalTableSetting
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** 全局设置仓库接口 */
interface GlobalSettingRepository {
    /** 获取用户的全局设置 */
    fun getGlobalSettingByUserId(userId: UUID): Flow<GlobalTableSetting?>

    /** 获取全局设置 */
    suspend fun getGlobalSettingById(id: UUID): GlobalTableSetting?

    /** 添加全局设置 */
    suspend fun addGlobalSetting(globalSetting: GlobalTableSetting): UUID

    /** 更新全局设置 */
    suspend fun updateGlobalSetting(globalSetting: GlobalTableSetting)

    /** 初始化用户的全局设置 如果用户没有全局设置，则创建一个默认全局设置 */
    suspend fun initDefaultSettingIfNeeded(userId: UUID): UUID

    /** 更新默认课表ID */
    suspend fun updateDefaultTableIds(userId: UUID, tableIds: List<UUID>)

    /** 获取默认课表ID */
    fun getDefaultTableIds(userId: UUID): Flow<List<UUID>>
}
