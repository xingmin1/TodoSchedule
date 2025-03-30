package com.example.todoschedule.domain.model

import com.example.todoschedule.core.constants.AppConstants

/**
 * 课程节点领域模型
 *
 * @property day 星期几 (1-7)
 * @property startNode 开始节次
 * @property step 课程节数
 * @property startWeek 开始周
 * @property endWeek 结束周
 * @property weekType 周类型 (0-全部, 1-单周, 2-双周)
 * @property room 教室
 * @property teacher 教师
 */
data class CourseNode(
    val day: Int,
    val startNode: Int,
    val step: Int,
    val startWeek: Int,
    val endWeek: Int,
    val weekType: Int,
    val room: String? = null,
    val teacher: String? = null
) {
    /** 是否在当前周显示 */
    fun isInWeek(week: Int): Boolean {
        return week in startWeek..endWeek &&
                when (weekType) {
                    AppConstants.WeekTypes.ALL -> true
                    AppConstants.WeekTypes.ODD -> week % 2 == 1
                    AppConstants.WeekTypes.EVEN -> week % 2 == 0
                    else -> false
                }
    }
}
