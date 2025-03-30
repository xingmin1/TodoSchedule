package com.example.todoschedule.data.repository

import android.util.Log
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.dao.GlobalSettingDao
import com.example.todoschedule.data.mapper.toGlobalTableSetting
import com.example.todoschedule.data.mapper.toGlobalTableSettingEntity
import com.example.todoschedule.domain.model.GlobalTableSetting
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** 全局设置仓库实现类 */
class GlobalSettingRepositoryImpl
@Inject
constructor(private val globalSettingDao: GlobalSettingDao) : GlobalSettingRepository {

    override fun getGlobalSettingByUserId(userId: Int): Flow<GlobalTableSetting?> {
        return globalSettingDao.getGlobalSettingByUserId(userId).map { it?.toGlobalTableSetting() }
    }

    override suspend fun getGlobalSettingById(id: Int): GlobalTableSetting? {
        return globalSettingDao.getGlobalSettingById(id)?.toGlobalTableSetting()
    }

    override suspend fun addGlobalSetting(globalSetting: GlobalTableSetting): Long {
        return globalSettingDao.insertGlobalSetting(globalSetting.toGlobalTableSettingEntity())
    }

    override suspend fun updateGlobalSetting(globalSetting: GlobalTableSetting) {
        globalSettingDao.updateGlobalSetting(globalSetting.toGlobalTableSettingEntity())
    }

    override suspend fun initDefaultSettingIfNeeded(userId: Int): Long {
        // 检查用户是否已有全局设置
        val hasGlobalSetting = globalSettingDao.hasGlobalSetting(userId) > 0
        if (!hasGlobalSetting) {
            // 创建默认全局设置
            val defaultGlobalSetting =
                GlobalTableSetting(
                    userId = userId,
                    defaultTableIds = emptyList(), // 初始为空，后续由创建默认课表后更新
                    showWeekend = true,
                    courseNotificationStyle = AppConstants.Notification.STYLE_SIMPLE,
                    notifyBeforeMinutes = AppConstants.Notification.DEFAULT_NOTIFY_MINUTES,
                    autoSwitchWeek = true,
                    showCourseTime = true
                )
            return addGlobalSetting(defaultGlobalSetting)
        }

        // 返回现有设置的ID
        try {
            val setting = globalSettingDao.getGlobalSettingByUserId(userId).first()
            return setting?.id?.toLong() ?: AppConstants.Ids.INVALID_SETTING_ID.toLong()
        } catch (e: Exception) {
            Log.e("GlobalSettingRepo", "获取用户设置ID失败: ${e.message}")
            return AppConstants.Ids.INVALID_SETTING_ID.toLong()
        }
    }

    override suspend fun updateDefaultTableIds(userId: Int, tableIds: List<Int>) {
        try {
            // 获取用户的全局设置
            val settingEntity = globalSettingDao.getGlobalSettingByUserId(userId).first()

            // 如果存在，则更新默认课表ID
            if (settingEntity != null) {
                val setting = settingEntity.toGlobalTableSetting()
                val updatedSetting = setting.copy(defaultTableIds = tableIds)
                updateGlobalSetting(updatedSetting)
            } else {
                Log.w("GlobalSettingRepo", "未找到用户 $userId 的全局设置，无法更新默认课表")
            }
        } catch (e: Exception) {
            Log.e("GlobalSettingRepo", "更新默认课表ID失败: ${e.message}")
        }
    }

    override fun getDefaultTableIds(userId: Int): Flow<List<Int>> {
        return globalSettingDao.getGlobalSettingByUserId(userId).map { entity ->
            entity?.let {
                it.defaultTableIds.split(",").filter { id -> id.isNotEmpty() }.map { id ->
                    id.toInt()
                }
            }
                ?: emptyList()
        }
    }
}
