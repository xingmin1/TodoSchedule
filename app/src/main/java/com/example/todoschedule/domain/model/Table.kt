package com.example.todoschedule.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** 课表领域模型 */
data class Table(
    val id: Int = 0,
    val userId: Int,
    val tableName: String,
    val background: String = "",
    val startDate: LocalDate,
    val totalWeeks: Int = 20,
    val showSat: Boolean = true,
    val showSun: Boolean = true
)

// 计算课表结束日期的扩展属性
/**
 * Table的扩展属性：自动计算课表的结束日期
 * 结束日期 = 起始日期 + (总周数×7 - 1)天
 */
val Table.endDate: LocalDate
    get() = startDate.plus((totalWeeks * 7 - 1).toLong(), DateTimeUnit.DAY)

/**
 * 多课表查找函数：根据日期查找该日期属于哪个课表
 * @param date 目标日期
 * @param tables 课表列表
 * @return 对应的Table对象，若无则返回null
 */
fun findTableForDate(date: LocalDate, tables: List<Table>): Table? {
    // 遍历所有课表，找到起始日期不晚于date且结束日期不早于date的课表
    return tables.findLast { table ->
        date >= table.startDate && date <= table.endDate
    }
}
