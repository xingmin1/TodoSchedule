package com.example.todoschedule.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalTime
import java.util.UUID

/**
 * 课表时间配置节点详情实体类。
 * 定义了每个节次的具体开始和结束时间。
 */
@Entity(
    tableName = "table_time_config_node_detaile", // 表名保持与 plan.md 一致
    foreignKeys = [
        ForeignKey(
            entity = TableTimeConfigEntity::class, // 关联到时间配置表
            parentColumns = ["id"],
            childColumns = ["table_time_config_id"],
            onDelete = ForeignKey.CASCADE // 如果时间配置删除，其节点也删除
        )
    ],
    indices = [Index(value = ["table_time_config_id"], name = "idx_nodedetail_configid")]
)
data class TableTimeConfigNodeDetaileEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(), // 本地ID，使用UUID的哈希值作为默认值
    @ColumnInfo(name = "table_time_config_id") val tableTimeConfigId: UUID, // 所属时间配置 ID
    val name: String, // 节次名称，例如 "第一大节", "上午自习"
    @ColumnInfo(name = "start_time") val startTime: LocalTime, // 开始时间
    @ColumnInfo(name = "end_time") val endTime: LocalTime, // 结束时间
    val node: Int // 代表这是第几节课 (例如 1, 2, 3...)
) 