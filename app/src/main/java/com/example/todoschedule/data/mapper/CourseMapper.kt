package com.example.todoschedule.data.mapper

import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.entity.CourseEntity
import com.example.todoschedule.data.database.entity.CourseNodeEntity
import com.example.todoschedule.data.model.CourseWithNodes
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.ui.theme.ColorSchemeEnum
import com.example.todoschedule.ui.theme.toColorSchemeEnum
import java.util.UUID

/**
 * 将CourseEntity转换为Course领域模型
 */
fun CourseWithNodes.toCourse(): Course {
    return Course(
        id = course.id,
        courseName = course.courseName,
        color = (course.color.toColorSchemeEnum()
            ?: AppConstants::DEFAULT_COURSE_COLOR) as ColorSchemeEnum,
        room = course.room,
        teacher = course.teacher,
        credit = course.credit,
        courseCode = course.courseCode,
        nodes = nodes.map { it.toCourseNode() } // 将节点转换为领域模型
    )
}

/**
 * 将Course领域模型转换为CourseEntity
 */
fun Course.toCourseEntity(tableId: UUID): CourseEntity {
    return CourseEntity(
        id = id,
        tableId = tableId,
        courseName = courseName,
        color = color.toString(),
        room = room,
        teacher = teacher,
        credit = credit,
        courseCode = courseCode
    )
}

fun Course.toCourseNodeEntity(courseId: UUID): List<CourseNodeEntity> {
    return nodes.map { node ->
        CourseNodeEntity(
            courseId = courseId,
            day = node.day,
            startNode = node.startNode,
            step = node.step,
            startWeek = node.startWeek,
            endWeek = node.endWeek,
            weekType = node.weekType,
            room = node.room,
            teacher = node.teacher
        )
    }
}

/**
 * 将CourseNodeEntity转换为CourseNode领域模型
 */
fun CourseNodeEntity.toCourseNode(): CourseNode {
    return CourseNode(
        day = day,
        startNode = startNode,
        step = step,
        startWeek = startWeek,
        endWeek = endWeek,
        weekType = weekType,
        room = room,
        teacher = teacher
    )
}

/**
 * 将CourseNode领域模型转换为CourseNodeEntity
 */
fun CourseNode.toCourseNodeEntity(courseId: UUID): CourseNodeEntity {
    return CourseNodeEntity(
        courseId = courseId,
        day = day,
        startNode = startNode,
        step = step,
        startWeek = startWeek,
        endWeek = endWeek,
        weekType = weekType,
        room = room,
        teacher = teacher
    )
} 