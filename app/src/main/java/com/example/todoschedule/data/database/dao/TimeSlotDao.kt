package com.example.todoschedule.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.data.database.entity.TimeSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeSlotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeSlot(timeSlot: TimeSlotEntity): Long // 插入单个时间槽

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeSlots(timeSlots: List<TimeSlotEntity>) // 插入多个时间槽

    @Update
    suspend fun updateTimeSlot(timeSlot: TimeSlotEntity) // 更新时间槽

    @Delete
    suspend fun deleteTimeSlot(timeSlot: TimeSlotEntity) // 删除时间槽

    @Query("SELECT * FROM time_slot WHERE id = :id")
    fun getTimeSlotById(id: Int): Flow<TimeSlotEntity?> // 根据 ID 获取时间槽

    @Query("SELECT * FROM time_slot")
    fun getAllTimeSlots(): Flow<List<TimeSlotEntity>> // 获取所有时间槽

    // 示例查询: 获取特定时间范围内的时间槽
    @Query("SELECT * FROM time_slot WHERE start_time >= :startTimeMillis AND end_time <= :endTimeMillis ORDER BY start_time ASC")
    fun getTimeSlotsInRange(startTimeMillis: Long, endTimeMillis: Long): Flow<List<TimeSlotEntity>>

    // 示例查询: 获取特定日程类型和 ID 的时间槽
    @Query("SELECT * FROM time_slot WHERE schedule_type = :scheduleType AND schedule_id = :scheduleId")
    fun getTimeSlotsBySchedule(
        scheduleType: ScheduleType,
        scheduleId: Int
    ): Flow<List<TimeSlotEntity>>

    // 删除特定日程类型和 ID 的时间槽
    @Query("DELETE FROM time_slot WHERE schedule_type = :scheduleType AND schedule_id = :scheduleId")
    suspend fun deleteTimeSlotsBySchedule(scheduleType: ScheduleType, scheduleId: Int)

    @Query("DELETE FROM time_slot")
    suspend fun deleteAllTimeSlots() // 删除所有时间槽

    // 获取指定用户的所有时间槽
    @Query("SELECT * FROM time_slot WHERE user_id = :userId ORDER BY start_time ASC")
    fun getTimeSlotsByUserId(userId: Int): Flow<List<TimeSlotEntity>>

    // 根据CRDT键获取时间槽（可观察）
    @Query("SELECT * FROM time_slot WHERE crdtKey = :crdtKey")
    fun getTimeSlotByCrdtKeyFlow(crdtKey: String): Flow<TimeSlotEntity?>

    // 根据CRDT键获取时间槽
    @Query("SELECT * FROM time_slot WHERE crdtKey = :crdtKey LIMIT 1")
    suspend fun getTimeSlotByCrdtKey(crdtKey: String): TimeSlotEntity?

    // 获取所有时间槽（非Flow）
    @Query("SELECT * FROM time_slot")
    suspend fun getAllTimeSlotsSync(): List<TimeSlotEntity>

    // 根据CRDT键获取ID
    @Query("SELECT id FROM time_slot WHERE crdtKey = :crdtKey LIMIT 1")
    suspend fun getIdByCrdtKey(crdtKey: String): Int?

    // 根据日程CRDT键获取时间槽列表
    @Query("SELECT * FROM time_slot WHERE schedule_crdt_key = :scheduleCrdtKey")
    suspend fun getTimeSlotsByScheduleCrdtKey(scheduleCrdtKey: String): List<TimeSlotEntity>
}