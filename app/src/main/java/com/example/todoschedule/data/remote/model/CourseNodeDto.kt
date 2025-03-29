package com.example.todoschedule.data.remote.model

/**
 * 课程节点数据传输对象
 */
data class CourseNodeDto(
    val id: Int = 0,
    val courseId: Int = 0,
    val courseNodeName: String? = null,
    val color: String? = null,
    val room: String? = null,
    val teacher: String? = null,
    val startNode: Int,
    val step: Int,
    val day: Int,
    val startWeek: Int,
    val endWeek: Int,
    val weekType: Int = 0 // 0-全部，1-单周，2-双周
) 