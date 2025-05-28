package com.example.todoschedule.data.sync

/**
 * 同步模块相关常量
 */
object SyncConstants {
    /**
     * 操作类型
     */
    object OperationType {
        const val ADD = "ADD"
        const val UPDATE = "UPDATE"
        const val DELETE = "DELETE"
    }

    /**
     * 实体类型
     */
    enum class EntityType(val value: String) {
        COURSE("Course"),
        COURSE_NODE("CourseNode"),
        ORDINARY_SCHEDULE("OrdinarySchedule"),
        TABLE("Table"),
        TIME_SLOT("TimeSlot"),
        TABLE_TIME_CONFIG("TableTimeConfig"),
        TABLE_TIME_CONFIG_NODE("TableTimeConfigNode"),
        GLOBAL_TABLE_SETTING("GlobalTableSetting")
    }

    /**
     * 同步状态
     */
    enum class SyncStatus(val value: Int) {
        PENDING(0),  // 待同步
        SYNCING(1),  // 同步中
        SYNCED(2),   // 已同步
        FAILED(3),   // 同步失败
        NOT_SYNCED(4) // 未同步（初始状态）
    }

    /**
     * API 相关常量
     */
    object Api {
        const val DEVICE_ID_HEADER = "X-Device-ID"
        const val SYNC_BASE_PATH = "/sync"
        const val DEVICE_REGISTER_PATH = "$SYNC_BASE_PATH/device/register"
        const val MESSAGES_PATH = "$SYNC_BASE_PATH/messages"

        // 默认同步间隔时间（毫秒）
        const val DEFAULT_SYNC_INTERVAL = 15 * 60 * 1000L // 15分钟
    }
} 