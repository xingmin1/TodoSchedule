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
import com.example.todoschedule.data.model.TableTimeConfigWithNodes // 需要创建这个关联模型
import kotlinx.coroutines.flow.Flow

@Dao
interface TableTimeConfigDao {

    // --- TableTimeConfig 操作 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeConfig(config: TableTimeConfigEntity): Long

    @Update
    suspend fun updateTimeConfig(config: TableTimeConfigEntity)

    @Delete
    suspend fun deleteTimeConfig(config: TableTimeConfigEntity)

    @Query("SELECT * FROM table_time_config WHERE id = :configId")
    fun getTimeConfigById(configId: Int): Flow<TableTimeConfigEntity?>

    @Query("SELECT * FROM table_time_config WHERE table_id = :tableId")
    fun getTimeConfigsForTable(tableId: Int): Flow<List<TableTimeConfigEntity>>

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

    // --- 关联查询 ---

    /**
     * 获取指定课表的默认时间配置及其所有节点详情。
     * 使用 @Transaction 保证原子性，@Relation 自动填充节点列表。
     */
    @Transaction
    @Query("SELECT * FROM table_time_config WHERE table_id = :tableId AND is_default = 1 LIMIT 1")
    fun getDefaultTimeConfigWithNodes(tableId: Int): Flow<TableTimeConfigWithNodes?>

    /**
     * 获取指定ID的时间配置及其所有节点详情。
     */
    @Transaction
    @Query("SELECT * FROM table_time_config WHERE id = :configId")
    fun getTimeConfigWithNodesById(configId: Int): Flow<TableTimeConfigWithNodes?>

    // --- 清理操作 ---
    @Query("DELETE FROM table_time_config WHERE table_id = :tableId")
    suspend fun deleteTimeConfigsForTable(tableId: Int) // 删除某个课表的所有时间配置

    @Query("DELETE FROM table_time_config")
    suspend fun deleteAllTimeConfigs()

    @Query("DELETE FROM table_time_config_node_detaile")
    suspend fun deleteAllNodeDetails()

} 