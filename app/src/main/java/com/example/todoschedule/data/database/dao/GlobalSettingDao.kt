package com.example.todoschedule.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todoschedule.data.database.entity.GlobalTableSettingEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** 全局设置DAO接口 */
@Dao
interface GlobalSettingDao {
    /** 获取用户的全局设置 */
    @Query("SELECT * FROM global_table_setting WHERE userId = :userId")
    fun getGlobalSettingByUserId(userId: UUID): Flow<GlobalTableSettingEntity?>

    /** 获取全局设置 */
    @Query("SELECT * FROM global_table_setting WHERE id = :id")
    suspend fun getGlobalSettingById(id: UUID): GlobalTableSettingEntity?

    /** 插入全局设置 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlobalSetting(globalSetting: GlobalTableSettingEntity): Long

    /** 更新全局设置 */
    @Update
    suspend fun updateGlobalSetting(globalSetting: GlobalTableSettingEntity)

    /** 检查用户是否有全局设置 */
    @Query("SELECT COUNT(*) FROM global_table_setting WHERE userId = :userId")
    suspend fun hasGlobalSetting(userId: UUID): Int
}
