package com.example.todoschedule.data.repository

import android.util.Log
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.core.extensions.valid
import com.example.todoschedule.data.database.dao.GlobalSettingDao
import com.example.todoschedule.data.mapper.toGlobalTableSetting
import com.example.todoschedule.data.mapper.toGlobalTableSettingEntity
import com.example.todoschedule.domain.model.GlobalTableSetting
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/** 全局设置仓库实现类 */
class GlobalSettingRepositoryImpl
@Inject
constructor(private val globalSettingDao: GlobalSettingDao) : GlobalSettingRepository {

    override fun getGlobalSettingByUserId(userId: UUID): Flow<GlobalTableSetting?> {
        return globalSettingDao.getGlobalSettingByUserId(userId).map { it?.toGlobalTableSetting() }
    }

    override suspend fun getGlobalSettingById(id: UUID): GlobalTableSetting? {
        return globalSettingDao.getGlobalSettingById(id)?.toGlobalTableSetting()
    }

    override suspend fun addGlobalSetting(globalSetting: GlobalTableSetting): UUID {
        assert(globalSetting.id.valid())
        globalSettingDao.insertGlobalSetting(globalSetting.toGlobalTableSettingEntity())
        return globalSetting.id
    }

    override suspend fun updateGlobalSetting(globalSetting: GlobalTableSetting) {
        globalSettingDao.updateGlobalSetting(globalSetting.toGlobalTableSettingEntity())
    }

    override suspend fun initDefaultSettingIfNeeded(userId: UUID): UUID {
        // 检查用户是否已有全局设置
        val hasGlobalSetting = globalSettingDao.hasGlobalSetting(userId) > 0
        if (!hasGlobalSetting) {
            // 创建默认全局设置
            val defaultGlobalSetting =
                GlobalTableSetting(
                    id = UUID.randomUUID(), // 生成新的ID
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
            return setting?.id ?: AppConstants.Ids.INVALID_SETTING_ID
        } catch (e: Exception) {
            Log.e("GlobalSettingRepo", "获取用户设置ID失败: ${e.message}")
            return AppConstants.Ids.INVALID_SETTING_ID
        }
    }

    override suspend fun updateDefaultTableIds(userId: UUID, tableIds: List<UUID>) {
        try {
            // 获取用户的全局设置实体
            val settingEntity = globalSettingDao.getGlobalSettingByUserId(userId).first()

            if (settingEntity != null) {
                // 找到了现有设置，更新它
                val setting = settingEntity.toGlobalTableSetting()
                val updatedSetting = setting.copy(defaultTableIds = tableIds)
                updateGlobalSetting(updatedSetting) // 调用 updateGlobalSetting
                Log.d("GlobalSettingRepo", "Updated defaultTableIds for existing user $userId")
            } else {
                // 未找到现有设置，创建新的设置记录
                Log.w(
                    "GlobalSettingRepo",
                    "未找到用户 $userId 的全局设置，将创建新的默认设置并设置默认课表"
                )
                val newDefaultSetting = GlobalTableSetting(
                    id = UUID.randomUUID(), // 生成新的ID
                    userId = userId,
                    defaultTableIds = tableIds, // 使用传入的 tableIds
                    // 使用与 initDefaultSettingIfNeeded 中相同的默认值
                    showWeekend = true,
                    courseNotificationStyle = AppConstants.Notification.STYLE_SIMPLE,
                    notifyBeforeMinutes = AppConstants.Notification.DEFAULT_NOTIFY_MINUTES,
                    autoSwitchWeek = true,
                    showCourseTime = true
                )
                // 调用 addGlobalSetting 来插入新记录
                addGlobalSetting(newDefaultSetting)
            }
        } catch (e: Exception) {
            Log.e("GlobalSettingRepo", "更新默认课表ID失败: ${e.message}")
            // 可以考虑向上抛出异常或返回一个结果
        }
    }

    override fun getDefaultTableIds(userId: UUID): Flow<List<UUID>> {
        return globalSettingDao.getGlobalSettingByUserId(userId).map { entity ->
            entity?.let {
                it.defaultTableIds.split(",").filter { id -> id.isNotEmpty() }.map { id ->
                    id.let { UUID.fromString(it) }
                }
            }
                ?: emptyList()
        }
    }
}
