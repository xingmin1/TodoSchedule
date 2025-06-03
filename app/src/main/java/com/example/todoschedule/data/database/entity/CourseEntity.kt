package com.example.todoschedule.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 课程实体类
 */
@Entity(
    tableName = "course",
    foreignKeys = [
        ForeignKey(
            entity = TableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tableId"), Index("id")]
)
data class CourseEntity(
    @PrimaryKey override val id: UUID = UUID.randomUUID(), // 本地ID，使用UUID的哈希值作为默认值
    val tableId: UUID, // 所属课表ID (本地ID，用于Room外键关系)
    val courseName: String, // 课程名称
    val color: String = "#FF4081", // 显示颜色，默认粉色
    val room: String? = null, // 教室位置
    val teacher: String? = null, // 授课教师
    val startWeek: Int = 1, // 开始周次
    val endWeek: Int = 20, // 结束周次
    val credit: Float? = null, // 学分
    val courseCode: String? = null, // 课程代码
    val syllabusLink: String? = null, // 教学大纲链接
) : Syncable