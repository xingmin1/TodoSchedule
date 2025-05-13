package com.example.todoschedule.ui.schedule

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import java.time.format.DateTimeFormatter

// 用于格式化时间的 Formatter（全局唯一，供所有内容文件import使用）
val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * LocalDateTime的扩展函数：格式化为字符串
 */
fun LocalDateTime.format(formatter: DateTimeFormatter): String {
    return this.toJavaLocalDateTime().format(formatter)
}

/**
 * 判断年份是否为闰年
 */
fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

/**
 * DayOfWeek的扩展函数：转为中文
 */
fun DayOfWeek.getChineseWeekName(): String {
    return when (this) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
}





