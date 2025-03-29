package com.example.todoschedule.domain.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * 日历工具类
 */
object CalendarUtils {
    
    // 学期开始日期 (可以从设置中读取)
    private val TERM_START_DATE: LocalDate = LocalDate.of(2023, 9, 1)
    
    // 最大周数
    const val MAX_WEEKS = 25
    
    /**
     * 获取当前周次
     */
    fun getCurrentWeek(): Int {
        val today = LocalDate.now()
        
        // 计算今天是学期开始后的第几天
        val daysFromStart = today.toEpochDay() - TERM_START_DATE.toEpochDay()
        if (daysFromStart < 0) return 1
        
        // 计算周数 (从1开始)
        return (daysFromStart / 7).toInt() + 1
    }
    
    /**
     * 获取指定周的日期列表
     */
    fun getWeekDates(week: Int): List<LocalDate> {
        // 计算该周的开始日期
        val weekStartDate = TERM_START_DATE.plusDays((week - 1) * 7L)
        
        // 确保以周一为一周的开始
        val mondayOfWeek = weekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        
        // 生成一周的日期
        return (0..6).map { mondayOfWeek.plusDays(it.toLong()) }
    }
    
    /**
     * 获取当前周的开始日期 (周一)
     */
    fun getCurrentWeekStart(): LocalDate {
        val today = LocalDate.now()
        val weekFields = WeekFields.of(Locale.getDefault())
        return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    
    /**
     * 获取日期对应的星期几 (1-7, 周一为1)
     */
    fun getDayOfWeek(date: LocalDate): Int {
        val day = date.dayOfWeek.value
        return day
    }
    
    /**
     * 格式化日期为月/日格式
     */
    fun formatDate(date: LocalDate): String {
        return "${date.monthValue}/${date.dayOfMonth}"
    }
} 