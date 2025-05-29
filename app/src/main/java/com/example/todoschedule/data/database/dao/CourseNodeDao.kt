package com.example.todoschedule.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todoschedule.data.database.entity.CourseNodeEntity
import kotlinx.coroutines.flow.Flow

/**
 * 课程节点DAO接口
 */
@Dao
interface CourseNodeDao {
    /**
     * 根据ID获取课程节点
     */
    @Query("SELECT * FROM course_node WHERE id = :id")
    suspend fun getCourseNodeById(id: Int): CourseNodeEntity?

    /**
     * 根据课程ID获取所有课程节点
     */
    @Query("SELECT * FROM course_node WHERE courseId = :courseId")
    fun getCourseNodesByCourseId(courseId: Int): Flow<List<CourseNodeEntity>>

    /**
     * 插入课程节点
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseNode(courseNode: CourseNodeEntity): Long

    /**
     * 批量插入课程节点
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseNodes(courseNodes: List<CourseNodeEntity>): List<Long>

    /**
     * 更新课程节点
     */
    @Update
    suspend fun updateCourseNode(courseNode: CourseNodeEntity)

    /**
     * 删除课程节点
     */
    @Delete
    suspend fun deleteCourseNode(courseNode: CourseNodeEntity)

    /**
     * 根据ID删除课程节点
     */
    @Query("DELETE FROM course_node WHERE id = :id")
    suspend fun deleteCourseNode(id: Int)

    /**
     * 删除课程的所有节点
     */
    @Query("DELETE FROM course_node WHERE courseId = :courseId")
    suspend fun deleteAllNodesOfCourse(courseId: Int)

    /**
     * 根据crdtKey获取课程节点ID
     */
    @Query("SELECT id FROM course_node WHERE crdtKey = :crdtKey LIMIT 1")
    suspend fun getIdByCrdtKey(crdtKey: String): Int?

    /**
     * 根据crdtKey获取课程节点
     */
    @Query("SELECT * FROM course_node WHERE crdtKey = :crdtKey LIMIT 1")
    suspend fun getCourseNodeByCrdtKey(crdtKey: String): CourseNodeEntity?

    /**
     * 根据课程crdtKey获取课程节点列表
     */
    @Query("SELECT * FROM course_node WHERE courseCrdtKey = :courseCrdtKey")
    suspend fun getCourseNodesByCourseCrdtKey(courseCrdtKey: String): List<CourseNodeEntity>
} 