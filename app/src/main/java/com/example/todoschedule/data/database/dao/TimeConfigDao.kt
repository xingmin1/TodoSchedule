package com.example.todoschedule.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.todoschedule.data.database.entity.TimeDetailEntity
import com.example.todoschedule.data.database.entity.TimeTableEntity
import com.example.todoschedule.data.model.TimeTableWithDetails
import kotlinx.coroutines.flow.Flow

/**
 * 时间配置DAO接口
 */
@Dao
interface TimeConfigDao {
    /**
     * 获取指定课表的时间配置
     */
    @Query("SELECT * FROM table_time_config WHERE tableId = :tableId")
    fun getTimeTablesByTableId(tableId: Int): Flow<List<TimeTableEntity>>

    /**
     * 获取指定课表的默认时间配置
     */
    @Query("SELECT * FROM table_time_config WHERE tableId = :tableId AND isDefault = 1 LIMIT 1")
    fun getDefaultTimeTable(tableId: Int): Flow<TimeTableEntity?>

    /**
     * 获取时间配置详情
     */
    @Transaction
    @Query("SELECT * FROM table_time_config WHERE id = :timeTableId")
    fun getTimeTableWithDetails(timeTableId: Int): Flow<TimeTableWithDetails?>

    /**
     * 获取节点详情
     */
    @Query("SELECT * FROM table_time_config_node_detaile WHERE tableTimeConfigId = :timeTableId ORDER BY node")
    fun getTimeDetailsByTimeTableId(timeTableId: Int): Flow<List<TimeDetailEntity>>

    /**
     * 插入时间配置
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeTable(timeTable: TimeTableEntity): Long

    /**
     * 插入节点详情
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeDetail(timeDetail: TimeDetailEntity)

    /**
     * 批量插入节点详情
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeDetails(timeDetails: List<TimeDetailEntity>)

    /**
     * 删除时间配置
     */
    @Query("DELETE FROM table_time_config WHERE id = :timeTableId")
    suspend fun deleteTimeTable(timeTableId: Int)
} 