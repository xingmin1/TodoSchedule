package com.example.todoschedule.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 课表时间节点详情实体类
 */
@Entity(
    tableName = "table_time_config_node_detaile",
    primaryKeys = ["tableTimeConfigId", "node"],
    foreignKeys = [
        ForeignKey(
            entity = TimeTableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableTimeConfigId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tableTimeConfigId")]
)
data class TimeDetailEntity(
    val tableTimeConfigId: Int, // 所属课表配置ID
    val node: Int, // 第几节课
    val name: String, // 节次名称(如第一节)
    val startTime: String, // 开始时间(如08:00)
    val endTime: String // 结束时间(如09:40)
) 