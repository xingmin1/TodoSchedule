package com.example.todoschedule.data.repository

import android.util.Log
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.dao.CourseDao
import com.example.todoschedule.data.mapper.toCourse
import com.example.todoschedule.data.mapper.toCourseEntity
import com.example.todoschedule.data.mapper.toCourseNode
import com.example.todoschedule.data.mapper.toCourseNodeEntity
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** 课程仓库实现类 */
class CourseRepositoryImpl
@Inject
constructor(
    private val courseDao: CourseDao,
    private val userRepository: UserRepository,
    private val globalSettingRepository: GlobalSettingRepository
) : CourseRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllCourses(): Flow<List<Course>> {
        return userRepository.getCurrentUser().flatMapLatest { user ->
            if (user != null) {
                globalSettingRepository.getDefaultTableIds(user.id).flatMapLatest { tableIds ->
                    val tableId = tableIds.firstOrNull() ?: AppConstants.Ids.INVALID_TABLE_ID
                    if (tableId == AppConstants.Ids.INVALID_TABLE_ID) {
                        // 如果没有有效的课表ID，记录日志并返回空列表
                        Log.w("CourseRepositoryImpl", "未找到默认课表ID，用户ID: ${user.id}")
                        flowOf(emptyList())
                    } else {
                        courseDao.getCoursesByTableId(tableId).map { courseWithNodes ->
                            courseWithNodes.map { it.toCourse() }
                        }
                    }
                }
            } else {
                // 如果没有用户，返回空列表
                Log.w("CourseRepositoryImpl", "未找到当前用户")
                flowOf(emptyList())
            }
        }
    }

    override suspend fun getCourseById(id: Int): Course? {
        return courseDao.getCourseWithNodesById(id)?.toCourse()
    }

    override suspend fun addCourse(course: Course, tableId: Int): Long {
        val courseId = courseDao.insertCourse(course.toCourseEntity(tableId))
        courseDao.insertCourseNodes(
            course.nodes.map { node -> node.toCourseNodeEntity(courseId.toInt()) }
        )
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
        return courseDao.getCoursesByTableId(tableId).map { courseWithNodesList ->
            courseWithNodesList.map { it.toCourse() }
        }
    }

    override fun getCoursesByWeek(tableId: Int, week: Int): Flow<List<Course>> {
        return courseDao.getCoursesByTableId(tableId).map { courseWithNodesList ->
            courseWithNodesList
                .map { courseWithNodes ->
                    val courseNodes =
                        courseWithNodes.nodes.filter { node ->
                            node.startWeek <= week &&
                                    node.endWeek >= week &&
                                    (node.weekType == AppConstants.WeekTypes.ALL ||
                                            (node.weekType == AppConstants.WeekTypes.ODD &&
                                                    week % 2 == 1) ||
                                            (node.weekType == AppConstants.WeekTypes.EVEN &&
                                                    week % 2 == 0))
                        }
                    courseWithNodes.copy(nodes = courseNodes).toCourse()
                }
                .filter { it.nodes.isNotEmpty() }
        }
    }

    override fun getCourseNodesByDayAndWeek(
        tableId: Int,
        day: Int,
        week: Int
    ): Flow<List<CourseNode>> {
        return courseDao.getCourseNodesByDayAndWeek(tableId, day, week).map { nodeEntities ->
            nodeEntities
                .filter { node ->
                    node.startWeek <= week &&
                            node.endWeek >= week &&
                            (node.weekType == AppConstants.WeekTypes.ALL ||
                                    (node.weekType == AppConstants.WeekTypes.ODD &&
                                            week % 2 == 1) ||
                                    (node.weekType == AppConstants.WeekTypes.EVEN &&
                                            week % 2 == 0))
                }
                .map { it.toCourseNode() }
        }
    }

    override suspend fun deleteCourseNode(nodeId: Int) {
        courseDao.deleteCourseNode(nodeId)
    }
}
