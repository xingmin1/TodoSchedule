package com.example.todoschedule.data.repository

import com.example.todoschedule.data.database.dao.CourseDao
import com.example.todoschedule.data.mapper.toCourse
import com.example.todoschedule.data.mapper.toCourseEntity
import com.example.todoschedule.data.mapper.toCourseNode
import com.example.todoschedule.data.mapper.toCourseNodeEntity
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.domain.repository.CourseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 课程仓库实现类
 */
class CourseRepositoryImpl @Inject constructor(
    private val courseDao: CourseDao,
) : CourseRepository {

    override fun getAllCourses(): Flow<List<Course>> {
        // TODO: 去掉下面`getCoursesByTableId`的硬编码参数`0`
        return courseDao.getCoursesByTableId(0)
            .map { courseWithNodes ->
                courseWithNodes.map { it.toCourse() }
            }
    }

    override suspend fun getCourseById(id: Int): Course? {
        return courseDao
            .getCourseWithNodesById(id)
            ?.toCourse()
    }

    override suspend fun addCourse(course: Course, tableId: Int): Long {
        val courseId = courseDao.insertCourse(course.toCourseEntity(tableId))
        courseDao.insertCourseNodes(course.nodes.map { node ->
            node.toCourseNodeEntity(courseId.toInt())
        })
        return courseId
    }

    override suspend fun updateCourse(course: Course, tableId: Int) {
        courseDao.updateCourse(course.toCourseEntity(tableId))
        // 先删除原有节点再添加新节点
        courseDao.deleteAllNodesOfCourse(course.id)
        course.nodes.forEach { node ->
            courseDao.insertCourseNode(node.toCourseNodeEntity(course.id))
        }
    }

    override suspend fun deleteCourse(courseId: Int) {
        courseDao.deleteCourse(courseId)
        courseDao.deleteAllNodesOfCourse(courseId)
    }

    override fun getCoursesByTableId(tableId: Int): Flow<List<Course>> {
        return courseDao.getCoursesByTableId(tableId).map {
            courseWithNodesList ->
            courseWithNodesList.map { it.toCourse() }
        }
    }

    override fun getCoursesByWeek(tableId: Int, week: Int): Flow<List<Course>> {
        return courseDao.getCoursesByTableId(tableId).map { courseWithNodesList ->
            courseWithNodesList
                .map {
                    courseWithNodes ->
                    val courseNodes = courseWithNodes.nodes
                        .filter { node ->
                            node.startWeek <= week && node.endWeek >= week &&
                                    (node.weekType == 0 ||
                                            (node.weekType == 1 && week % 2 == 1) ||
                                            (node.weekType == 2 && week % 2 == 0))
                        }
                    courseWithNodes
                        .copy(nodes = courseNodes)
                        .toCourse()
                }
                .filter { it.nodes.isNotEmpty() }
        }
    }

    override fun getCourseNodesByDayAndWeek(tableId: Int, day: Int, week: Int): Flow<List<CourseNode>> {
        return courseDao.getCourseNodesByDayAndWeek(tableId, day, week).map { nodeEntities ->
            nodeEntities
                .filter { node ->
                    node.startWeek <= week && node.endWeek >= week &&
                    (node.weekType == 0 ||
                    (node.weekType == 1 && week % 2 == 1) ||
                    (node.weekType == 2 && week % 2 == 0))
                }
                .map { it.toCourseNode() }
        }
    }

    override suspend fun deleteCourseNode(nodeId: Int) {
        courseDao.deleteCourseNode(nodeId)
    }
} 