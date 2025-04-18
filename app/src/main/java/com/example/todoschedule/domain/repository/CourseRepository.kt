package com.example.todoschedule.domain.repository

import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import kotlinx.coroutines.flow.Flow

/**
 * 课程仓库接口
 */
interface CourseRepository {
    /**
     * 获取所有课程
     */
    fun getCurrentUserAllCourses(): Flow<List<Course>>

    /**
     * 根据ID获取课程
     */
    suspend fun getCourseById(id: Int): Course?

    /**
     * 添加课程，返回课程ID
     */
    suspend fun addCourse(course: Course, tableId: Int): Long

    /**
     * 批量添加课程，返回课程ID列表
     */
    suspend fun addCourses(course: List<Course>, tableId: Int): List<Long>

    /**
     * 更新课程
     */
    suspend fun updateCourse(course: Course, tableId: Int)

    /**
     * 删除课程
     */
    suspend fun deleteCourse(courseId: Int)

    /**
     * 获取指定课表的所有课程
     */
    fun getCoursesByTableId(tableId: Int): Flow<List<Course>>

    /**
     * 获取特定周次的课程
     */
    fun getCoursesByWeek(tableId: Int, week: Int): Flow<List<Course>>

    /**
     * 获取特定日期的课程节点
     */
    fun getCourseNodesByDayAndWeek(tableId: Int, day: Int, week: Int): Flow<List<CourseNode>>

    /**
     * 删除课程节点
     */
    suspend fun deleteCourseNode(nodeId: Int)
}
