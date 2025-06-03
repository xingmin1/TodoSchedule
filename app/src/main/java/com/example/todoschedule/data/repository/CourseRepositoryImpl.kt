package com.example.todoschedule.data.repository

import android.util.Log
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.core.extensions.valid
import com.example.todoschedule.data.database.dao.CourseDao
import com.example.todoschedule.data.database.dao.TableDao
import com.example.todoschedule.data.mapper.toCourse
import com.example.todoschedule.data.mapper.toCourseEntity
import com.example.todoschedule.data.mapper.toCourseNode
import com.example.todoschedule.data.mapper.toCourseNodeEntity
import com.example.todoschedule.data.sync.SyncConstants
import com.example.todoschedule.data.sync.SyncManager
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** 课程仓库实现类 */
class CourseRepositoryImpl
@Inject
constructor(
    private val courseDao: CourseDao,
    private val tableDao: TableDao,
    private val sessionRepository: SessionRepository,
    private val globalSettingRepository: GlobalSettingRepository,
    private val syncManager: SyncManager
) : CourseRepository {

    private val TAG = "CourseRepositoryImpl"

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCurrentUserAllCourses(): Flow<List<Course>> {
        return sessionRepository.currentUserIdFlow.flatMapLatest { userId ->
            if (userId != null && userId != AppConstants.Ids.INVALID_USER_ID) {
                globalSettingRepository.getDefaultTableIds(userId)
                    .flatMapLatest { tableIds ->
                    val tableId = tableIds.firstOrNull() ?: AppConstants.Ids.INVALID_TABLE_ID
                    if (tableId == AppConstants.Ids.INVALID_TABLE_ID) {
                        Log.w(TAG, "未找到默认课表ID，用户ID: $userId")
                        flowOf(emptyList())
                    } else {
                        courseDao.getCoursesByTableId(tableId).map { courseWithNodes ->
                            courseWithNodes.map { it.toCourse() }
                        }
                    }
                }
            } else {
                Log.w(TAG, "未找到当前用户或用户未登录 (userId: $userId)")
                flowOf(emptyList())
            }
        }
    }

    override suspend fun getCourseById(id: UUID): Course? {
        return courseDao.getCourseWithNodesById(id)?.toCourse()
    }

    override suspend fun addCourse(course: Course, tableId: UUID): UUID {
        try {
            val courseEntity = course.toCourseEntity(tableId)
            val courseId = courseEntity.id
            assert(courseId.valid()) {
                "课程ID不能为空"
            }
            courseDao.insertCourse(courseEntity)
            courseDao.insertCourseNodes(
                course.nodes.map { node -> node.toCourseNodeEntity(courseId) }
            )

            // 创建同步消息
            val userId = sessionRepository.currentUserIdFlow.value!!
            syncManager.createAndSaveSyncMessage(
                entityType = SyncConstants.EntityType.COURSE,
                operationType = SyncConstants.OperationType.ADD,
                userId = userId,
                entity = courseEntity
            )

            Log.d(TAG, "添加课程已创建同步消息: ${courseEntity.id}")
            return courseId
        } catch (e: Exception) {
            Log.e(TAG, "添加课程及同步消息失败", e)
            throw e
        }
    }

    override suspend fun addCourses(course: List<Course>, tableId: UUID): List<UUID> {
        Log.d(TAG, "开始导入 ${course.size} 个课程到表ID=${tableId}")

        try {
            // 1. 获取表信息，为课程设置正确的CRDT键
            val table = tableDao.getTableById(tableId).first()
            val tableCrdtKey = table!!.id

            // 2. 正确设置表的CRDT键
            val courseEntities = course.map {
                it.toCourseEntity(tableId).copy(tableId = tableCrdtKey)
            }

            // 3. 插入课程并获取ID
            val courseIds = courseEntities.map {
                assert(it.id.valid()) {
                    "课程ID不能为空"
                }
                it.id
            }
            courseDao.insertCourses(courseEntities)
            Log.d(TAG, "成功插入 ${courseIds.size} 个课程")

            // 4. 插入课程节点
            val allNodeEntities =
                mutableListOf<com.example.todoschedule.data.database.entity.CourseNodeEntity>()
            course.forEachIndexed { index, courseItem ->
                if (index < courseIds.size) {
                    val courseId = courseIds[index]
                    val courseCrdtKey = courseEntities[index].id

                    val nodeEntities = courseItem.nodes.map { node ->
                        node.toCourseNodeEntity(courseId).copy(courseId = courseCrdtKey)
                    }

                    courseDao.insertCourseNodes(nodeEntities)
                    allNodeEntities.addAll(nodeEntities)
                    Log.d(
                        TAG,
                        "为课程 '${courseItem.courseName}' 插入了 ${nodeEntities.size} 个节点"
                    )
                }
            }
//
            // 6. 在单独协程中创建同步消息和触发同步
            CoroutineScope(Dispatchers.IO).launch {
                delay(1000) // 关键延迟，确保数据库操作完成和UI更新
                try {
                    // 创建课程同步消息
                    val userId = sessionRepository.currentUserIdFlow.value!!
                    for (entity in courseEntities) {
                        syncManager.createAndSaveSyncMessage(
                            entityType = SyncConstants.EntityType.COURSE,
                            operationType = SyncConstants.OperationType.ADD,
                            userId = userId,
                            entity = entity
                        )
                    }

                    // 创建课程节点同步消息
                    for (nodeEntity in allNodeEntities) {
                        syncManager.createAndSaveSyncMessage(
                            entityType = SyncConstants.EntityType.COURSE_NODE,
                            operationType = SyncConstants.OperationType.ADD,
                            userId = userId,
                            entity = nodeEntity
                        )
                    }
                    // 触发同步
                    syncManager.syncNow(ignoreExceptions = true)
                } catch (e: Exception) {
                    Log.e(TAG, "创建同步消息或触发同步失败: ${e.message}", e)
                }
            }
            return courseIds
        } catch (e: Exception) {
            Log.e(TAG, "批量添加课程失败: ${e.message}", e)
            throw e
        }
    }

    override suspend fun updateCourse(course: Course, tableId: UUID) {
        try {
            val courseEntity = course.toCourseEntity(tableId)
            courseDao.updateCourse(courseEntity)
            courseDao.deleteAllNodesOfCourse(course.id)

            course.nodes.forEach { node ->
                courseDao.insertCourseNode(node.toCourseNodeEntity(course.id))
            }

            // 创建同步消息
            val userId = sessionRepository.currentUserIdFlow.value!!
            syncManager.createAndSaveSyncMessage(
                entityType = SyncConstants.EntityType.COURSE,
                operationType = SyncConstants.OperationType.UPDATE,
                userId = userId,
                entity = courseEntity
            )

            Log.d(TAG, "更新课程已创建同步消息: ${courseEntity.id}")
        } catch (e: Exception) {
            Log.e(TAG, "更新课程及同步消息失败", e)
            throw e
        }
    }

    override suspend fun deleteCourse(courseId: UUID) {
        try {
            // 获取实体信息，以便删除后也能创建同步消息
            val courseEntity = courseDao.getCourseWithNodesById(courseId)?.course

            // 删除课程及其节点
            courseDao.deleteCourse(courseId)
            courseDao.deleteAllNodesOfCourse(courseId)

            // 创建同步消息
            if (courseEntity != null) {
                val userId = sessionRepository.currentUserIdFlow.value!!
                syncManager.createAndSaveSyncMessage(
                    entityType = SyncConstants.EntityType.COURSE,
                    operationType = SyncConstants.OperationType.DELETE,
                    userId = userId,
                    entity = courseEntity
                )

                Log.d(TAG, "删除课程已创建同步消息: ${courseEntity.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除课程及同步消息失败", e)
            throw e
        }
    }

    override fun getCoursesByTableId(tableId: UUID): Flow<List<Course>> {
        return courseDao.getCoursesByTableId(tableId).map { courseWithNodesList ->
            courseWithNodesList.map { it.toCourse() }
        }
    }

    override fun getCoursesByWeek(tableId: UUID, week: Int): Flow<List<Course>> {
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
        tableId: UUID,
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

    override suspend fun deleteCourseNode(nodeId: UUID) {
        try {
            // 由于节点的删除关联到课程，所以需要查询节点所属的课程
            // 同时有必要随后更新课程的同步信息
            val node = courseDao.getCourseNodeById(nodeId)
            courseDao.deleteCourseNode(nodeId)

            if (node != null) {
                // 获取更新后的课程信息
                val courseEntity = courseDao.getCourseById(node.courseId)
                if (courseEntity != null) {
                    // 创建同步消息
                    val userId = sessionRepository.currentUserIdFlow.value!!
                    syncManager.createAndSaveSyncMessage(
                        entityType = SyncConstants.EntityType.COURSE,
                        operationType = SyncConstants.OperationType.UPDATE,
                        userId = userId,
                        entity = courseEntity
                    )

                    Log.d(TAG, "删除课程节点后更新课程同步消息: ${courseEntity.id}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除课程节点及同步消息失败", e)
            throw e
        }
    }
}
