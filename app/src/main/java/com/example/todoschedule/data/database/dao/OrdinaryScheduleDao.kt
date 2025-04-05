package com.example.todoschedule.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.todoschedule.data.database.entity.OrdinaryScheduleEntity
import com.example.todoschedule.data.model.OrdinaryScheduleWithTimeSlots
import kotlinx.coroutines.flow.Flow

@Dao
interface OrdinaryScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: OrdinaryScheduleEntity): Long // 返回插入行的 ID

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<OrdinaryScheduleEntity>): List<Long> // 插入多个日程

    @Update
    suspend fun updateSchedule(schedule: OrdinaryScheduleEntity) // 更新日程

    @Delete
    suspend fun deleteSchedule(schedule: OrdinaryScheduleEntity) // 删除日程

    @Transaction
    @Query("SELECT * FROM ordinary_schedule WHERE id = :id")
    // 根据ID获取一个普通日程及其所有关联的时间槽 (使用 @Relation)。
    // 注意：返回的时间槽需要通过日程类型过滤
    fun getScheduleWithTimeSlotsById(id: Int): Flow<OrdinaryScheduleWithTimeSlots?>

    @Transaction
    @Query(
        """
        SELECT * 
        FROM ordinary_schedule 
        WHERE userId = :userId
        ORDER BY id DESC
        """
    )
    fun getAllSchedulesWithTimeSlots(userId: Int): Flow<List<OrdinaryScheduleWithTimeSlots>>

    @Query("DELETE FROM ordinary_schedule WHERE userId = :userId")
    suspend fun deleteAllSchedules(userId: Int) // 删除所有日程
}