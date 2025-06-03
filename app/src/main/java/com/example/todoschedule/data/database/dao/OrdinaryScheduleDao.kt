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
import java.util.UUID

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

    @Query("DELETE FROM ordinary_schedule WHERE id = :id")
    suspend fun deleteOrdinarySchedule(id: UUID) // 根据ID删除日程

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrdinarySchedule(schedule: OrdinaryScheduleEntity): Long // 插入日程（用于同步）

    @Transaction
    @Query("SELECT * FROM ordinary_schedule WHERE id = :id")
    // 根据ID获取一个普通日程及其所有关联的时间槽 (使用 @Relation)。
    // 注意：返回的时间槽需要通过日程类型过滤
    fun getScheduleWithTimeSlotsById(id: UUID): Flow<OrdinaryScheduleWithTimeSlots?>

    @Transaction
    @Query(
        """
        SELECT * 
        FROM ordinary_schedule 
        WHERE userId = :userId
        ORDER BY id DESC
        """
    )
    fun getAllSchedulesWithTimeSlots(userId: UUID): Flow<List<OrdinaryScheduleWithTimeSlots>>

    @Query("DELETE FROM ordinary_schedule WHERE userId = :userId")
    suspend fun deleteAllSchedules(userId: UUID) // 删除所有日程

    @Query("SELECT id FROM ordinary_schedule WHERE id = :id LIMIT 1")
    suspend fun getIdById(id: String): Int? // 根据CRDT键获取ID

    @Query("SELECT * FROM ordinary_schedule WHERE id = :id LIMIT 1")
    suspend fun getOrdinaryScheduleById(id: String): OrdinaryScheduleEntity? // 根据CRDT键获取实体

    @Query("SELECT * FROM ordinary_schedule WHERE userId = :userId")
    suspend fun getOrdinarySchedulesByUserId(userId: String): List<OrdinaryScheduleEntity> // 根据用户CRDT键获取日程列表

    /**
     * 根据用户ID获取日程列表（非Flow）
     */
    @Query("SELECT * FROM ordinary_schedule WHERE userId = :userId")
    suspend fun fetchSchedulesByUserId(userId: UUID): List<OrdinaryScheduleEntity>

    /**
     * 根据ID获取日程实体（非Flow）
     */
    @Query("SELECT * FROM ordinary_schedule WHERE id = :scheduleId LIMIT 1")
    suspend fun getOrdinaryScheduleById(scheduleId: UUID): OrdinaryScheduleEntity?
}