package com.example.todoschedule.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.todoschedule.data.database.entity.TableTimeConfigEntity
import com.example.todoschedule.data.database.entity.TableTimeConfigNodeDetaileEntity
import com.example.todoschedule.data.model.TableTimeConfigWithNodes // 确保这个关联模型已创建
import kotlinx.coroutines.flow.Flow

@Dao
interface TableTimeConfigDao {

    // --- TableTimeConfig 操作 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeConfig(config: TableTimeConfigEntity): Long // 返回新插入行的 ID

    @Update
    suspend fun updateTimeConfig(config: TableTimeConfigEntity)

    @Delete // Note: This only deletes the config, not the nodes. Use transaction for full delete.
    suspend fun deleteTimeConfigEntity(config: TableTimeConfigEntity)

    @Query("SELECT * FROM table_time_config WHERE id = :configId")
    fun getTimeConfigByIdFlow(configId: Int): Flow<TableTimeConfigEntity?> // Renamed to avoid clash

    @Query("SELECT * FROM table_time_config WHERE id = :configId")
    suspend fun getTimeConfigById(configId: Int): TableTimeConfigEntity? // Suspend version for checks

    @Query("SELECT * FROM table_time_config WHERE table_id = :tableId")
    fun getTimeConfigsForTableFlow(tableId: Int): Flow<List<TableTimeConfigEntity>> // Renamed

    @Query("SELECT * FROM table_time_config WHERE table_id = :tableId")
    suspend fun getTimeConfigsForTable(tableId: Int): List<TableTimeConfigEntity> // Suspend version

    @Query("SELECT * FROM table_time_config WHERE table_id = :tableId AND is_default = 1 LIMIT 1")
    suspend fun getDefaultTimeConfigForTable(tableId: Int): TableTimeConfigEntity?

    @Query("UPDATE table_time_config SET is_default = 0 WHERE table_id = :tableId AND is_default = 1")
    suspend fun clearDefaultFlagForTable(tableId: Int)

    @Query("UPDATE table_time_config SET is_default = 1 WHERE id = :configId")
    suspend fun setDefaultFlag(configId: Int)


    // --- TableTimeConfigNodeDetaile 操作 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodeDetail(node: TableTimeConfigNodeDetaileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodeDetails(nodes: List<TableTimeConfigNodeDetaileEntity>)

    @Update
    suspend fun updateNodeDetail(node: TableTimeConfigNodeDetaileEntity)

    @Delete
    suspend fun deleteNodeDetail(node: TableTimeConfigNodeDetaileEntity)

    @Query("DELETE FROM table_time_config_node_detaile WHERE table_time_config_id = :configId")
    suspend fun deleteNodeDetailsForConfig(configId: Int)

    // --- 关联查询 (Flow based for observation) ---

    /** 获取指定课表的默认时间配置及其所有节点详情。 */
    @Transaction
    @Query("SELECT * FROM table_time_config WHERE table_id = :tableId AND is_default = 1 LIMIT 1")
    fun getDefaultTimeConfigWithNodes(tableId: Int): Flow<TableTimeConfigWithNodes?>

    /** 获取指定ID的时间配置及其所有节点详情。 */
    @Transaction
    @Query("SELECT * FROM table_time_config WHERE id = :configId")
    fun getTimeConfigWithNodesById(configId: Int): Flow<TableTimeConfigWithNodes> // Returns Flow<List>

    /** 获取指定课表的所有时间配置及其所有节点详情。 */
    @Transaction
    @Query("SELECT * FROM table_time_config WHERE table_id = :tableId ORDER BY is_default DESC, id ASC") // Default first
    fun getAllTimeConfigsWithNodes(tableId: Int): Flow<List<TableTimeConfigWithNodes>>


    // --- 事务性操作 ---

    /**
     * 插入一个新的时间配置及其节点详情。
     * @param configEntity 配置实体
     * @param nodeEntities 节点实体列表
     * @return 新配置的 ID
     */
    @Transaction
    suspend fun insertConfigWithNodes(configEntity: TableTimeConfigEntity, nodeEntities: List<TableTimeConfigNodeDetaileEntity>): Long {
        val configId = insertTimeConfig(configEntity)
        // Ensure nodes have the correct configId (Room might handle this if ID is autoGenerate)
        val updatedNodes = nodeEntities.map { it.copy(tableTimeConfigId = configId.toInt()) }
        insertNodeDetails(updatedNodes)
        return configId
    }

    /**
     * 更新一个时间配置及其节点详情。
     * 会先删除旧的节点，再插入新的节点。
     * @param configEntity 要更新的配置实体 (ID 必须有效)
     * @param nodeEntities 新的节点实体列表
     */
    @Transaction
    suspend fun updateConfigWithNodes(configEntity: TableTimeConfigEntity, nodeEntities: List<TableTimeConfigNodeDetaileEntity>) {
        updateTimeConfig(configEntity) // Update the config details (e.g., name)
        deleteNodeDetailsForConfig(configEntity.id) // Delete old nodes
        // Ensure new nodes have the correct configId
        val updatedNodes = nodeEntities.map { it.copy(tableTimeConfigId = configEntity.id) }
        insertNodeDetails(updatedNodes) // Insert new nodes
    }

    /**
     * 删除一个时间配置及其所有关联的节点详情。
     * @param configId 要删除的配置 ID。
     */
    @Transaction
    suspend fun deleteConfigWithNodes(configId: Int) {
        deleteNodeDetailsForConfig(configId) // Delete nodes first
        // Create a dummy entity just for the delete operation by ID
        val configToDelete = TableTimeConfigEntity(id = configId, tableId = -1, name = "", isDefault = false) // tableId and name don't matter here
        deleteTimeConfigEntity(configToDelete) // Delete the config itself
    }

    /**
     * 设置指定课表的默认时间配置。
     * @param tableId 课表 ID
     * @param newDefaultConfigId 要设为默认的配置 ID
     */
    @Transaction
    suspend fun setDefault(tableId: Int, newDefaultConfigId: Int) {
        clearDefaultFlagForTable(tableId) // Clear the old default flag
        setDefaultFlag(newDefaultConfigId) // Set the new default flag
    }


    // --- 清理操作 ---
    @Query("DELETE FROM table_time_config WHERE table_id = :tableId")
    suspend fun deleteTimeConfigsForTable(tableId: Int) // 删除某个课表的所有时间配置

    @Query("DELETE FROM table_time_config")
    suspend fun deleteAllTimeConfigs()

    @Query("DELETE FROM table_time_config_node_detaile")
    suspend fun deleteAllNodeDetails()

}
