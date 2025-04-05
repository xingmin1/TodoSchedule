package com.example.todoschedule.domain.repository

import com.example.todoschedule.domain.model.TableTimeConfig
import kotlinx.coroutines.flow.Flow

/** 课表时间配置仓库接口。 */
interface TableTimeConfigRepository {

    /** 获取指定课表的默认时间配置。 返回的 Flow 在没有找到默认配置时会发射 null。 */
    fun getDefaultTimeConfig(tableId: Int): Flow<TableTimeConfig?>

    /** 根据 ID 获取时间配置。 */
    fun getTimeConfigById(configId: Int): Flow<TableTimeConfig?>

    /**
     * 为指定的课表创建或确保存在一个默认的时间配置。
     * 如果已存在默认配置，则不执行任何操作。
     * 返回创建或找到的默认配置的 ID。
     * 如果创建失败，则返回 null 或抛出异常。
     */
    suspend fun ensureDefaultTimeConfig(tableId: Int, userId: Int): Int?

    // 可以根据需要添加其他方法，例如:
    // suspend fun addTimeConfig(config: TableTimeConfig)
    // suspend fun updateTimeConfig(config: TableTimeConfig)
    // suspend fun deleteTimeConfig(configId: Int)
    // fun getAllTimeConfigsForTable(tableId: Int): Flow<List<TableTimeConfig>>

}
