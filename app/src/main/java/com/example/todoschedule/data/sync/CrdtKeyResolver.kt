package com.example.todoschedule.data.sync

import android.util.Log
import com.example.todoschedule.data.database.AppDatabase
import com.example.todoschedule.data.database.entity.CourseEntity
import com.example.todoschedule.data.database.entity.CourseNodeEntity
import com.example.todoschedule.data.database.entity.OrdinaryScheduleEntity
import com.example.todoschedule.data.database.entity.TableEntity
import com.example.todoschedule.data.repository.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * CRDT键解析器
 *
 * 负责解析CRDT实体间的引用关系，将分布式键映射到本地数据库ID
 */
@Singleton
class CrdtKeyResolver @Inject constructor(
    private val database: AppDatabase,
    private val syncRepositoryProvider: Provider<SyncRepository>
) {
    companion object {
        private const val TAG = "CrdtKeyResolver"
    }

    // 获取SyncRepository实例时使用provider.get()，延迟加载
    private val syncRepository: SyncRepository
        get() = syncRepositoryProvider.get()

    /**
     * 根据Table的crdtKey查找对应的本地ID
     *
     * @param crdtKey 课表的CRDT键
     * @return 本地数据库中的ID，如果未找到则返回null
     */
    suspend fun resolveTableId(crdtKey: String?): Int? = withContext(Dispatchers.IO) {
        if (crdtKey == null) return@withContext null
        return@withContext database.tableDao().getIdByCrdtKey(crdtKey)
    }

    /**
     * 根据Course的crdtKey查找对应的本地ID
     *
     * @param crdtKey 课程的CRDT键
     * @return 本地数据库中的ID，如果未找到则返回null
     */
    suspend fun resolveCourseId(crdtKey: String?): Int? = withContext(Dispatchers.IO) {
        if (crdtKey == null) return@withContext null
        return@withContext database.courseDao().getIdByCrdtKey(crdtKey)
    }

    /**
     * 解析课程引用关系
     *
     * @param course 课程实体
     * @return 解析后的课程实体，若无法解析返回null
     */
    suspend fun resolveCourseReferences(course: CourseEntity): CourseEntity? {
        try {
            // 确保tableCrdtKey不为空
            val tableCrdtKey = course.tableCrdtKey ?: run {
                Log.w(TAG, "Course tableCrdtKey is null for course ${course.crdtKey}")
                return null
            }

            // 解析课表引用
            val table = syncRepository.getTableDao().getTableByCrdtKey(tableCrdtKey)
            if (table == null) {
                Log.w(
                    TAG,
                    "Cannot resolve table with crdtKey $tableCrdtKey for course ${course.crdtKey}"
                )
                return null
            }

            // 返回解析后的课程，将CRDT键映射到本地数据库ID
            return course.copy(
                tableId = table.id
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving course references for ${course.crdtKey}", e)
            return null
        }
    }

    /**
     * 解析课程节点引用关系
     *
     * @param courseNode 课程节点实体
     * @return 解析后的课程节点实体，若无法解析返回null
     */
    suspend fun resolveCourseNodeReferences(courseNode: CourseNodeEntity): CourseNodeEntity? {
        try {
            // 确保courseCrdtKey不为空
            val courseCrdtKey = courseNode.courseCrdtKey ?: run {
                Log.w(TAG, "CourseNode courseCrdtKey is null for node ${courseNode.crdtKey}")
                return null
            }

            // 解析课程引用
            val course = syncRepository.getCourseDao().getCourseByCrdtKey(courseCrdtKey)
            if (course == null) {
                Log.w(
                    TAG,
                    "Cannot resolve course with crdtKey $courseCrdtKey for course node ${courseNode.crdtKey}"
                )
                return null
            }

            // 返回解析后的课程节点，将CRDT键映射到本地数据库ID
            return courseNode.copy(
                courseId = course.id
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving course node references for ${courseNode.crdtKey}", e)
            return null
        }
    }

    /**
     * 解析普通日程引用关系
     *
     * @param schedule 普通日程实体
     * @return 解析后的普通日程实体，若无法解析返回null
     */
    suspend fun resolveOrdinaryScheduleReferences(schedule: OrdinaryScheduleEntity): OrdinaryScheduleEntity? {
        try {
            // 普通日程直接关联用户ID，不需要解析额外的CRDT键
            // 在真实场景中，如果有用户CRDT键，这里需要解析用户引用

            // 简单验证用户ID
            if (schedule.userId <= 0) {
                Log.w(
                    TAG,
                    "Invalid userId ${schedule.userId} for ordinary schedule ${schedule.crdtKey}"
                )
                return null
            }

            return schedule
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving ordinary schedule references for ${schedule.crdtKey}", e)
            return null
        }
    }

    /**
     * 解析课表引用关系
     *
     * @param table 课表实体
     * @return 解析后的课表实体，若无法解析返回null
     */
    suspend fun resolveTableReferences(table: TableEntity): TableEntity? {
        try {
            // 课表直接关联用户ID，不需要解析额外的CRDT键
            // 在真实场景中，如果有用户CRDT键，这里需要解析用户引用

            // 简单验证用户ID
            if (table.userId <= 0) {
                Log.w(TAG, "Invalid userId ${table.userId} for table ${table.crdtKey}")
                return null
            }

            return table
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving table references for ${table.crdtKey}", e)
            return null
        }
    }
} 