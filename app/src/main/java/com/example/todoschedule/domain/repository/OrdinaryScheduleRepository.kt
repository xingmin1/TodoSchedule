package com.example.todoschedule.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 普通日程仓库接口，定义了普通日程相关的数据操作。
 */
interface OrdinaryScheduleRepository {

    /**
     * 插入一个普通日程及其关联的时间槽。
     * @param schedule 普通日程的 Domain 模型。
     * @return 插入的普通日程的 ID。
     */
    suspend fun insertSchedule(schedule: com.example.todoschedule.domain.model.OrdinarySchedule): Long

    /**
     * 插入多个普通日程及其关联的时间槽。
     * @param schedules 普通日程的 Domain 模型列表。
     */
    suspend fun insertSchedules(schedules: List<com.example.todoschedule.domain.model.OrdinarySchedule>)

    /**
     * 更新一个普通日程及其关联的时间槽。
     * @param schedule 普通日程的 Domain 模型。
     */
    suspend fun updateSchedule(schedule: com.example.todoschedule.domain.model.OrdinarySchedule)

    /**
     * 删除一个普通日程及其关联的时间槽。
     * @param schedule 普通日程的 Domain 模型。
     */
    suspend fun deleteSchedule(schedule: com.example.todoschedule.domain.model.OrdinarySchedule)

    /**
     * 根据 ID 获取一个普通日程及其关联的时间槽。
     * @param id 日程 ID。
     * @return 包含日程和时间槽的 Flow<OrdinarySchedule?>。
     */
    fun getScheduleById(Id: UUID): Flow<com.example.todoschedule.domain.model.OrdinarySchedule?>

    /**
     * 获取所有普通日程及其关联的时间槽。
     * @return 包含所有日程和时间槽的 Flow<List<OrdinarySchedule>>。
     */
    fun getAllSchedules(userId: UUID): Flow<List<com.example.todoschedule.domain.model.OrdinarySchedule>>

    /**
     * 删除所有普通日程及其关联的时间槽。
     */
    suspend fun deleteAllSchedules(userId: UUID)
} 