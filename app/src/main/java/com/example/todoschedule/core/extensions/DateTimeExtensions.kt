package com.example.todoschedule.core.extensions

import kotlinx.datetime.LocalDateTime
import java.util.Calendar

/**
 * 将 kotlinx.datetime.LocalDateTime 转换为 java.util.Calendar
 */
fun LocalDateTime.toJavaCalendar(): Calendar {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthNumber - 1) // Calendar月份从0开始
        set(Calendar.DAY_OF_MONTH, dayOfMonth)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, second)
        set(Calendar.MILLISECOND, nanosecond / 1_000_000)
    }
}