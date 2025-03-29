package com.example.todoschedule.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todoschedule.data.database.entity.TableEntity
import kotlinx.coroutines.flow.Flow

/**
 * 课表DAO接口
 */
@Dao
interface TableDao {
    /**
     * 获取所有课表
     */
    @Query("SELECT * FROM `table` ORDER BY listPosition")
    fun getAllTables(): Flow<List<TableEntity>>

    /**
     * 获取默认课表（目前仅取第一个）
     */
    @Query("SELECT * FROM `table` ORDER BY id LIMIT 1")
    fun getDefaultTable(): Flow<TableEntity?>

    /**
     * 根据ID获取课表
     */
    @Query("SELECT * FROM `table` WHERE id = :tableId")
    suspend fun getTableById(tableId: Int): TableEntity?

    /**
     * 插入课表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: TableEntity): Long

    /**
     * 更新课表
     */
    @Update
    suspend fun updateTable(table: TableEntity)

    /**
     * 删除课表
     */
    @Query("DELETE FROM `table` WHERE id = :tableId")
    suspend fun deleteTable(tableId: Int)
} 