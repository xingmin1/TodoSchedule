package com.example.todoschedule.data.mapper

import com.example.todoschedule.data.database.entity.CourseEntity
import com.example.todoschedule.data.database.entity.CourseNodeEntity
import com.example.todoschedule.data.model.CourseWithNodes
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode

/**
 * 将CourseEntity转换为Course领域模型
 */
fun CourseWithNodes.toCourse(): Course {
    return Course(
        id = course.id,
        courseName = course.courseName,
        color = course.color,
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
fun Course.toCourseEntity(tableId: Int): CourseEntity {
    return CourseEntity(
        id = id,
        tableId = tableId,
        courseName = courseName,
        color = color,
        room = room,
        teacher = teacher,
        credit = credit,
        courseCode = courseCode
    )
}

fun Course.toCourseNodeEntity(courseId: Int): List<CourseNodeEntity> {
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
fun CourseNode.toCourseNodeEntity(courseId: Int): CourseNodeEntity {
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