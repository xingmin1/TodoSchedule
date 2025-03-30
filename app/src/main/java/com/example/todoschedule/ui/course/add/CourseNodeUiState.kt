package com.example.todoschedule.ui.course.add

/**
 * 课程节点UI状态
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
data class CourseNodeUiState(
    val day: Int,
    val startNode: Int,
    val step: Int,
    val startWeek: Int,
    val endWeek: Int,
    val weekType: Int,
    val room: String = "",
    val teacher: String = ""
) 