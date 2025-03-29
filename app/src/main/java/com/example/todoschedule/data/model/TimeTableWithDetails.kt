package com.example.todoschedule.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.todoschedule.data.database.entity.TimeDetailEntity
import com.example.todoschedule.data.database.entity.TimeTableEntity

/**
 * 时间配置与详情关联模型
 */
data class TimeTableWithDetails(
    @Embedded val timeTable: TimeTableEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "tableTimeConfigId"
    )
    val details: List<TimeDetailEntity> = emptyList()
) 