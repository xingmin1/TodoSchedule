package com.example.todoschedule.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoschedule.data.database.converter.ReminderType
import com.example.todoschedule.data.database.converter.ScheduleType
import java.util.UUID

@Entity(
    tableName = "time_slot",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"], name = "idx_timeslot_userid")]
)
data class TimeSlotEntity(
    @PrimaryKey override val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "user_id") val userId: UUID,
    @ColumnInfo(name = "start_time") val startTime: Long, // 开始时间 (时间戳)
    @ColumnInfo(name = "end_time") val endTime: Long, // 结束时间 (时间戳)
    @ColumnInfo(name = "schedule_type") val scheduleType: ScheduleType,
    @ColumnInfo(name = "schedule_id") val scheduleId: UUID, // 关联日程的ID (课程、作业、普通日程等)
    val head: String? = null, // 可选的标题覆盖
    val priority: Int? = null, // 优先级 (考虑使用枚举)
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false, // 是否已完成
    @ColumnInfo(name = "is_repeated") val isRepeated: Boolean = false, // 是否重复
    @ColumnInfo(name = "repeat_pattern") val repeatPattern: String? = null, // 重复模式
    @ColumnInfo(name = "reminder_type") val reminderType: ReminderType? = ReminderType.NONE,
    @ColumnInfo(name = "reminder_offset") val reminderOffset: Long? = null, // 提醒偏移量 (相对开始时间的毫秒数)
) : Syncable