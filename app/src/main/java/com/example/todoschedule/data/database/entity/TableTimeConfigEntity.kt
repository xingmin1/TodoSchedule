package com.example.todoschedule.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 课表时间配置实体类。
 * 每个课表可以有多个时间配置（例如，夏季作息、冬季作息）。
 */
@Entity(
    tableName = "table_time_config",
    foreignKeys = [
        ForeignKey(
            entity = TableEntity::class, // 关联到课表表
            parentColumns = ["id"],
            childColumns = ["table_id"],
            onDelete = ForeignKey.CASCADE // 如果课表删除，其时间配置也删除
        )
    ],
    indices = [Index(value = ["table_id"], name = "idx_timeconfig_tableid")]
)
data class TableTimeConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "table_id") val tableId: Int, // 所属课表 ID
    val name: String, // 配置名称，例如 "默认作息", "夏季作息"
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false // 是否为该课表的默认配置
) 