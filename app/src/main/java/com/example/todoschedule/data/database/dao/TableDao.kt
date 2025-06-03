package com.example.todoschedule.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todoschedule.data.database.entity.TableEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** 课表DAO接口 */
@Dao
interface TableDao {
    /** 获取所有课表 */
    @Query("SELECT * FROM `table` ORDER BY listPosition")
    fun getAllTables(): Flow<List<TableEntity>>

    /** 获取默认课表（目前仅取第一个） */
    @Query("SELECT * FROM `table` ORDER BY id LIMIT 1")
    fun getDefaultTable(): Flow<TableEntity?>

    /** [Flow] 根据ID观察课表 */
    @Query("SELECT * FROM `table` WHERE id = :tableId")
    fun getTableById(tableId: UUID): Flow<TableEntity?>

    /** [Flow] 根据用户ID观察课表 */
    @Query("SELECT * FROM `table` WHERE userId = :userId")
    fun getTableByUserId(userId: UUID): Flow<List<TableEntity>>

    /** [Suspend] 根据ID获取课表（一次性） */
    @Query("SELECT * FROM `table` WHERE id = :tableId")
    suspend fun fetchTableById(tableId: UUID): TableEntity?

    /** [Suspend] 根据用户ID获取课表（一次性） */
    @Query("SELECT * FROM `table` WHERE userId = :userId")
    suspend fun fetchTablesByUserId(userId: UUID): List<TableEntity>

    /** 插入课表 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: TableEntity): UUID

    /** 更新课表 */
    @Update
    suspend fun updateTable(table: TableEntity)

    /** 删除课表 */
    @Query("DELETE FROM `table` WHERE id = :tableId")
    suspend fun deleteTable(tableId: UUID)

    /** 根据id查询课表本地ID */
    @Query("SELECT id FROM `table` WHERE id = :id LIMIT 1")
    suspend fun getIdById(id: String): Int?

    /** 根据id查询课表实体 */
    @Query("SELECT * FROM `table` WHERE id = :id LIMIT 1")
    suspend fun getTableById(id: String): TableEntity?

    /** 根据userId查询课表列表 */
    @Query("SELECT * FROM `table` WHERE userId = :userId")
    suspend fun getTablesByUserId(userId: String): List<TableEntity>

    /** 获取所有课表(非Flow) */
    @Query("SELECT * FROM `table` ORDER BY listPosition")
    suspend fun getAllTablesSync(): List<TableEntity>
}
