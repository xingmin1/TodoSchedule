package com.example.todoschedule.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.todoschedule.data.database.entity.CourseEntity
import com.example.todoschedule.data.database.entity.CourseNodeEntity
import com.example.todoschedule.data.model.CourseWithNodes
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 课程DAO接口
 */
@Dao
interface CourseDao {
    /**
     * 获取指定课表的所有课程
     */
    @Transaction
    @Query("SELECT * FROM course WHERE tableId = :tableId")
    fun getCoursesByTableId(tableId: UUID): Flow<List<CourseWithNodes>>

    /**
     * 获取特定周次的课程
     */
    @Transaction
    @Query(
        """
        SELECT DISTINCT c.* FROM course c
        JOIN course_node cn ON c.id = cn.courseId
        WHERE c.tableId = :tableId
        AND cn.startWeek <= :week AND cn.endWeek >= :week
        AND (cn.weekType = 0 OR (cn.weekType = 1 AND :week % 2 = 1) OR (cn.weekType = 2 AND :week % 2 = 0))
    """
    )
    fun getCoursesByWeek(tableId: UUID, week: Int): Flow<List<CourseWithNodes>>

    /**
     * 获取特定日期的课程节点
     */
    @Query(
        """
        SELECT * FROM course_node
        WHERE courseId IN (SELECT id FROM course WHERE tableId = :tableId)
        AND day = :day
        AND startWeek <= :week AND endWeek >= :week
        AND (weekType = 0 OR (weekType = 1 AND :week % 2 = 1) OR (weekType = 2 AND :week % 2 = 0))
    """
    )
    fun getCourseNodesByDayAndWeek(tableId: UUID, day: Int, week: Int): Flow<List<CourseNodeEntity>>

    /**
     * 获取课程详情
     */
    @Transaction
    @Query("SELECT * FROM course WHERE id = :courseId")
    suspend fun getCourseWithNodesById(courseId: UUID): CourseWithNodes?

    /**
     * 插入课程
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): UUID

    /**
     * 批量插入课程
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>): List<UUID>

    /**
     * 插入课程节点
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseNode(courseNode: CourseNodeEntity): UUID

    /**
     * 批量插入课程节点
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseNodes(courseNodes: List<CourseNodeEntity>)

    /**
     * 更新课程
     */
    @Update
    suspend fun updateCourse(course: CourseEntity)

    /**
     * 删除课程
     */
    @Query("DELETE FROM course WHERE id = :courseId")
    suspend fun deleteCourse(courseId: UUID)

    /**
     * 删除课程节点
     */
    @Query("DELETE FROM course_node WHERE id = :nodeId")
    suspend fun deleteCourseNode(nodeId: UUID)

    /**
     * 根据ID获取课程实体（非Flow）
     */
    @Query("SELECT * FROM course WHERE id = :id LIMIT 1")
    suspend fun getCourseByIdValue(id: UUID): CourseEntity?

    /**
     * 根据CRDT键查询课程
     */
    @Query("SELECT * FROM course WHERE id = :id LIMIT 1")
    suspend fun getCourseById(id: String): CourseEntity?

    /**
     * 获取所有课程
     */
    @Query("SELECT * FROM course")
    suspend fun getAllCourses(): List<CourseEntity>

    /**
     * 删除课程的所有节点
     */
    @Query("DELETE FROM course_node WHERE courseId = :courseId")
    suspend fun deleteAllNodesOfCourse(courseId: UUID)

    /**
     * 根据id查询课程本地ID
     */
    @Query("SELECT id FROM course WHERE id = :id LIMIT 1")
    suspend fun getIdById(id: String): Int?

    /**
     * 根据id查询课程节点
     */
    @Query("SELECT * FROM course_node WHERE id = :id LIMIT 1")
    suspend fun getCourseNodeById(id: String): CourseNodeEntity?

    /**
     * 根据tableId查询课程列表
     */
    @Query("SELECT * FROM course WHERE tableId = :tableId")
    suspend fun getCoursesByTableId(tableId: String): List<CourseEntity>

    /**
     * 根据ID获取课程节点
     */
    @Query("SELECT * FROM course_node WHERE id = :nodeId")
    suspend fun getCourseNodeById(nodeId: UUID): CourseNodeEntity?

    /**
     * 根据ID获取课程实体（不含节点）
     */
    @Query("SELECT * FROM course WHERE id = :courseId")
    suspend fun getCourseById(courseId: UUID): CourseEntity?
}
