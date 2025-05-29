package com.example.todoschedule.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tap.hlc.Timestamp
import java.util.UUID

/**
 * 课程节点实体类
 */
@Entity(
    tableName = "course_node",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId"), Index("crdtKey"), Index("courseCrdtKey")]
)
data class CourseNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseId: Int, // 所属课程ID (本地ID，用于Room外键关系)
    val courseNodeName: String? = null, // 课程节点名称
    val color: String? = null, // 显示颜色
    val room: String? = null, // 教室位置
    val teacher: String? = null, // 授课教师
    val startNode: Int, // 开始节次
    val step: Int, // 课程长度(节数)
    val day: Int, // 星期几(1-7)
    val startWeek: Int, // 开始周次
    val endWeek: Int, // 结束周次
    val weekType: Int = 0, // 周类型(0-全部，1-单周，2-双周)

    // 同步字段
    val crdtKey: String = UUID.randomUUID().toString(), // CRDT唯一标识符
    val courseCrdtKey: String? = null, // 课程的CRDT唯一标识符
    @ColumnInfo(name = "update_timestamp")
    val updateTimestamp: Long? = null // 更新时间戳
) : Syncable {
    override val syncId: String
        get() = crdtKey
} 