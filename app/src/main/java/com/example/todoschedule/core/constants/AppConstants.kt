package com.example.todoschedule.core.constants

import com.example.todoschedule.ui.navigation.AppRoutes
import kotlinx.datetime.LocalDate

/** 应用中使用的常量 */
object AppConstants {
    /** ID相关常量 */
    object Ids {
        // 无效的ID值，用于表示未找到或未设置的ID
        const val INVALID_ID = -1
        const val INVALID_USER_ID = -1
        const val INVALID_TABLE_ID = -1
        const val INVALID_SETTING_ID = -1
        const val INVALID_COURSE_ID = -1
    }

    /** 周类型常量 */
    object WeekTypes {
        const val ALL = 0 // 每周
        const val ODD = 1 // 单周
        const val EVEN = 2 // 双周
    }

    /** 状态常量 */
    object Status {
        const val PENDING = 0 // 待处理
        const val IN_PROGRESS = 1 // 进行中
        const val COMPLETED = 2 // 已完成
    }

    /** 优先级常量 */
    object Priority {
        const val LOW = 0
        const val MEDIUM = 1
        const val HIGH = 2
    }

    /** 通知相关常量 */
    object Notification {
        const val DEFAULT_NOTIFY_MINUTES = 15 // 默认提前多少分钟提醒

        // 通知样式
        const val STYLE_SIMPLE = 0 // 简洁样式
        const val STYLE_DETAILED = 1 // 详细样式
        const val STYLE_SILENT = 2 // 静默样式
    }

    /** 数据库相关常量 */
    object Database {
        const val DB_NAME = "todo_schedule.db" // 数据库名称
        const val DB_VERSION = 2 // 数据库版本
        const val DEFAULT_TABLE_NAME = "默认课表" // 默认课表名称
        val DEFAULT_TABLE_START_DATE: LocalDate =
            LocalDate(2025, 2, 17) // 默认课表开始日期
        const val DEFAULT_USER_NAME = "默认用户" // 默认用户名称
        const val DEFAULT_TIME_CONFIG_TABLE_NAME = "默认时间配置表" // 默认时间配置表名称
    }

    /** 路由相关常量 */
    object Routes {
        val START_SCREEN = AppRoutes.Home
    }
}
