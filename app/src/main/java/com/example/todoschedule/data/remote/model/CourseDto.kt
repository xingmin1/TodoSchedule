package com.example.todoschedule.data.remote.model

import java.util.UUID

/**
 * 课程数据传输对象
 */
data class CourseDto(
    val id: UUID,
    val tableId: UUID,
    val courseName: String,
    val color: String = "#FF4081",
    val room: String? = null,
    val teacher: String? = null,
    val credit: Float? = null,
    val courseCode: String? = null,
    val syllabusLink: String? = null,
    val nodes: List<CourseNodeDto> = emptyList()
) 