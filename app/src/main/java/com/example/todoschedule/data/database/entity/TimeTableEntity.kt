package com.example.todoschedule.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 课表时间配置实体类
 */
@Entity(
    tableName = "table_time_config",
    foreignKeys = [
        ForeignKey(
            entity = TableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tableId")]
)
data class TimeTableEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tableId: Int, // 所属课表ID
    val name: String, // 课表配置名称
    val isDefault: Boolean = true // 是否默认
) 