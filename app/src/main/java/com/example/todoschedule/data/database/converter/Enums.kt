package com.example.todoschedule.data.database.converter

/**
 * 普通日程的状态枚举。
 */
enum class ScheduleStatus {
    TODO, // 待办
    IN_PROGRESS, // 进行中
    DONE // 已完成
}

/**
 * 日程类型枚举。
 */
enum class ScheduleType {
    ORDINARY, // 普通日程
    COURSE, // 课程
    HOMEWORK, // 作业
    EXAM, // 考试
    REVIEW, // 复习
    ONLINE_COURSE // 网课
    // 可以根据需要添加更多类型
}

/**
 * 提醒类型枚举。
 */
enum class ReminderType {
    NOTIFICATION, // 通知栏提醒
    ALARM, // 闹钟提醒
    NONE // 无提醒
}

/**
 * Room 类型转换器，用于处理枚举类型和数据库存储类型之间的转换。
 */
class DatabaseConverters {
}