package com.example.todoschedule.domain.repository

import com.example.todoschedule.domain.model.TimeDetail
import kotlinx.coroutines.flow.Flow

/**
 * 时间配置仓库接口
 */
interface TimeConfigRepository {
    /**
     * 获取指定课表的时间节点
     */
    fun getTimeDetailsByTableId(tableId: Int): Flow<List<TimeDetail>>

    /**
     * 创建默认时间配置
     */
    suspend fun createDefaultTimeConfig(tableId: Int): Long
} 