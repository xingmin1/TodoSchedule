package com.example.todoschedule.data.database.entity

import androidx.room.ColumnInfo
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
            parentColumns = ["crdtKey"],
            childColumns = ["tableCrdtKey"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tableId"), Index("crdtKey"), Index("tableCrdtKey")]
)
data class CourseEntity(
    val courseName: String, // 课程名称
    val color: String = "#FF4081", // 显示颜色，默认粉色
    val room: String? = null, // 教室位置
    val teacher: String? = null, // 授课教师
    val startWeek: Int = 1, // 开始周次
    val endWeek: Int = 20, // 结束周次
    val credit: Float? = null, // 学分
    val courseCode: String? = null, // 课程代码
    val syllabusLink: String? = null, // 教学大纲链接

    // 同步字段
    @PrimaryKey val crdtKey: String = UUID.randomUUID().toString(), // CRDT唯一标识符
    val tableCrdtKey: String? = null, // 课表的CRDT唯一标识符
    @ColumnInfo(name = "update_timestamp")
    val updateTimestamp: Long? = null // 更新时间戳
) : Syncable {
    override val syncId: String
        get() = crdtKey
    val id : Int
        get() = crdtKey.toInt()
} 