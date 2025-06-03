package com.example.todoschedule.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import java.util.UUID

/** 课表实体类 */
@Entity(
    tableName = "table",
    foreignKeys =
        [
            ForeignKey(
                entity = UserEntity::class,
                parentColumns = ["id"],
                childColumns = ["userId"],
                onDelete = ForeignKey.CASCADE
            )],
    indices = [Index("userId"), Index("id")]
)
data class TableEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(), // 本地ID，使用UUID的哈希值作为默认值
    val userId: UUID, // 用户ID (本地ID，用于Room外键关系)
    val tableName: String, // 课表名称
    val background: String = "", // 背景颜色
    val listPosition: Int = 0, // 列表中的位置
    val terms: String = "", // 学期信息
    val startDate: LocalDate, // 学期开始日期
    val totalWeeks: Int = 20, // 总周数
) : Syncable {
    override val syncId: String
        get() = id.toString()
}
