package com.example.todoschedule.domain.model

import com.example.todoschedule.ui.theme.ColorSchemeEnum

/**
 * 课程领域模型
 *
 * @property courseName 课程名称
 * @property color 颜色
 * @property room 教室
 * @property teacher 教师
 * @property credit 学分
 * @property courseCode 课程代码
 * @property nodes 课程节点
 */
data class Course(
    val id: Int = 0,
    val courseName: String,
    val color: ColorSchemeEnum,
    val room: String? = null,
    val teacher: String? = null,
    val credit: Float? = null,
    val courseCode: String? = null,
    val nodes: List<CourseNode> = emptyList()
) 