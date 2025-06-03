package com.example.todoschedule.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.todoschedule.data.database.converter.ScheduleStatus
import java.util.UUID

@Entity(
    tableName = "ordinary_schedule",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"]), Index("id")]
)
data class OrdinaryScheduleEntity(
    @PrimaryKey override val id: UUID = UUID.randomUUID(), // 本地ID，使用UUID的哈希值作为默认值
    val userId: UUID, // 用户ID (本地ID，用于Room外键关系)
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val category: String? = null,
    val color: String? = null,
    @ColumnInfo(name = "is_all_day") val isAllDay: Boolean = false,
    val status: ScheduleStatus? = ScheduleStatus.TODO,
) : Syncable