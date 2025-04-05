package com.example.todoschedule.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.todoschedule.data.database.converter.ScheduleStatus

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
    indices = [androidx.room.Index(value = ["userId"])]
)
data class OrdinaryScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val category: String? = null,
    val color: String? = null,
    @ColumnInfo(name = "is_all_day") val isAllDay: Boolean = false,
    val status: ScheduleStatus? = ScheduleStatus.TODO
) 