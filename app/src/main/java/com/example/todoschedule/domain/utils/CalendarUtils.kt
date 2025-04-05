package com.example.todoschedule.domain.utils

import com.example.todoschedule.core.constants.AppConstants
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * 日历工具类
 */
object CalendarUtils {
    // 最大周数
    const val MAX_WEEKS = 25

    /**
     * 获取当前周次
     *
     * @param termStartDate 学期开始日期，若为null则使用默认日期
     * @return 当前周次
     */
    fun getCurrentWeek(termStartDate: LocalDate? = null): Int {
        val startDate = termStartDate ?: AppConstants.Database.DEFAULT_TABLE_START_DATE
        val today = Clock.System.now()
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date

        // 计算今天是学期开始后的第几天
        val daysFromStart = today.toEpochDays() - startDate.toEpochDays()
        if (daysFromStart < 0) return 1

        // 计算周数 (从1开始)
        return (daysFromStart / 7) + 1
    }

    /**
     * 获取指定周的日期列表
     *
     * @param week 周次
     * @param termStartDate 学期开始日期，若为null则使用默认日期
     * @return 该周的日期列表（周一到周日）
     */
    fun getWeekDates(week: Int, termStartDate: LocalDate? = null): List<LocalDate> {
        val startDate = termStartDate ?: AppConstants.Database.DEFAULT_TABLE_START_DATE

        // 计算该周的开始日期
        val weekStartDate = startDate.plus((week - 1) * 7, DateTimeUnit.DAY)

        // 确保以周一为一周的开始
        val mondayOfWeek = getWeekStartDate(weekStartDate)

        // 生成一周的日期
        return List(7) { i -> mondayOfWeek.plus(i, DateTimeUnit.DAY) }
    }

    /**
     * 获取一周的开始日期（周一）
     */
    fun getWeekStartDate(date: LocalDate): LocalDate {
        val dayOfWeek = date.dayOfWeek.isoDayNumber
        // 如果不是周一，回退到本周的周一
        return if (dayOfWeek == 1) date else date.minus(DatePeriod(days = dayOfWeek - 1))
    }

    /**
     * 获取当前周的开始日期 (周一)
     */
    fun getCurrentWeekStart(): LocalDate {
        val today = Clock.System.now()
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        return getWeekStartDate(today)
    }

    /**
     * 获取日期对应的星期几 (1-7, 周一为1)
     */
    fun getDayOfWeek(date: LocalDate): Int {
        return date.dayOfWeek.isoDayNumber
    }

    /**
     * 格式化日期为月/日格式
     */
    fun formatDate(date: LocalDate): String {
        return "${date.monthNumber}/${date.dayOfMonth}"
    }
} 