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
    indices = [Index(value = ["userId"]), Index("crdtKey"), Index("userCrdtKey")]
)
data class OrdinaryScheduleEntity(
    @PrimaryKey val id: Int = UUID.randomUUID().hashCode(), // 本地ID，使用UUID的哈希值作为默认值
    val userId: Int, // 用户ID (本地ID，用于Room外键关系)
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val category: String? = null,
    val color: String? = null,
    @ColumnInfo(name = "is_all_day") val isAllDay: Boolean = false,
    val status: ScheduleStatus? = ScheduleStatus.TODO,

    // 同步字段
    val crdtKey: String = UUID.randomUUID().toString(), // CRDT唯一标识符
    val userCrdtKey: String? = null, // 用户的CRDT唯一标识符
    @ColumnInfo(name = "update_timestamp")
    val updateTimestamp: Long? = null // 更新时间戳
) : Syncable {
    override val syncId: String
        get() = crdtKey
} 