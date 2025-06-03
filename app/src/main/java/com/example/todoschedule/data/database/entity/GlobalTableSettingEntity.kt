package com.example.todoschedule.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** 全局课表设置实体类 */
@Entity(
    tableName = "global_table_setting",
    foreignKeys =
        [
            ForeignKey(
                entity = UserEntity::class,
                parentColumns = ["id"],
                childColumns = ["userId"],
                onDelete = ForeignKey.CASCADE
            )],
    indices = [Index("userId")],
)
data class GlobalTableSettingEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(), // 本地ID，使用UUID的哈希值作为默认值
    val userId: UUID, // 用户ID
    val defaultTableIds: String, // 默认显示课表ID，使用TypeConverter转换
    val showWeekend: Boolean = true, // 显示周末
    val courseNotificationStyle: Int = 0, // 课程通知风格
    val notifyBeforeMinutes: Int = 15, // 提前提醒分钟数
    val autoSwitchWeek: Boolean = true, // 自动切换周次
    val showCourseTime: Boolean = true, // 显示课程时间
)
