package com.example.todoschedule.domain.repository

import com.example.todoschedule.domain.model.TableTimeConfig
import kotlinx.coroutines.flow.Flow

/** 课表时间配置仓库接口。 */
interface TableTimeConfigRepository {

    /** 获取指定课表的默认时间配置。 返回的 Flow 在没有找到默认配置时会发射 null。 */
    fun getDefaultTimeConfig(tableId: UUID): Flow<TableTimeConfig?>

    /** 根据 TableTimeConfig ID 获取时间配置。 如果找不到，Flow 会发出 null。 */
    fun getTimeConfigById(configId: UUID): Flow<TableTimeConfig?> // Keep as nullable for consistency with DAO

    /**
     * 为指定的课表创建或确保存在一个默认的时间配置。
     * 如果已存在默认配置，则不执行任何操作。
     * 返回创建或找到的默认配置的 ID。
     * 如果创建失败，则返回 null。
     */
    suspend fun ensureDefaultTimeConfig(
        tableId: UUID,
        userId: UUID
    ): Int? // userId might not be needed here if not used for ownership/permissions

    /**
     * 添加一个新的时间配置。
     * @param config 要添加的时间配置（包含节点信息）。 `id` 应为 0 或 null。
     * @return 新创建的时间配置的 ID。
     * @throws IllegalArgumentException 如果 `config.isDefault` 为 true (应使用 `setDefaultTimeConfig` 方法)。
     */
    suspend fun addTimeConfig(config: TableTimeConfig): Long

    /**
     * 更新一个已存在的时间配置。
     * 这会替换掉该配置的所有节点信息。
     * @param config 要更新的时间配置（包含新的节点信息）。 `id` 必须是有效的。
     * @throws IllegalArgumentException 如果尝试修改 `isDefault` 状态 (应使用 `setDefaultTimeConfig` 方法)。
     * @throws NoSuchElementException 如果具有给定 `config.id` 的配置不存在。
     */
    suspend fun updateTimeConfig(config: TableTimeConfig)

    /**
     * 删除一个时间配置及其所有节点。
     * @param configId 要删除的时间配置的 ID。
     * @throws IllegalArgumentException 如果尝试删除默认配置。可以先设置其他配置为默认。
     * @throws NoSuchElementException 如果具有给定 `configId` 的配置不存在。
     */
    suspend fun deleteTimeConfig(configId: UUID)

    /**
     * 获取指定课表的所有时间配置（包括默认和自定义）。
     * @param tableId 课表的 ID。
     * @return 包含该课表所有时间配置的 Flow。
     */
    fun getAllTimeConfigsForTable(tableId: UUID): Flow<List<TableTimeConfig>>

    /**
     * 将指定的时间配置设置为其所属课表的默认配置。
     * 原来的默认配置（如果存在）将被取消默认状态。
     * @param tableId 课表的 ID。
     * @param configId 要设置为默认的时间配置的 ID。
     * @throws NoSuchElementException 如果具有给定 `configId` 或 `tableId` 的配置不存在。
     */
    suspend fun setDefaultTimeConfig(tableId: UUID, configId: UUID)
}

