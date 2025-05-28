package com.example.todoschedule.data.sync.adapter

import com.example.todoschedule.data.sync.SyncConstants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synk适配器注册表
 *
 * 负责管理和提供所有实体类型的适配器
 */
@Singleton
class SynkAdapterRegistry @Inject constructor(
    private val courseAdapter: CourseAdapter,
    private val tableAdapter: TableAdapter,
    private val courseNodeAdapter: CourseNodeAdapter,
    private val ordinaryScheduleAdapter: OrdinaryScheduleAdapter,
    private val timeSlotAdapter: TimeSlotAdapter
) {

    /**
     * 根据实体类型获取对应的适配器
     *
     * @param entityType 实体类型
     * @return 对应类型的适配器
     * @throws IllegalArgumentException 如果找不到对应类型的适配器
     */
    fun getAdapter(entityType: String): SynkAdapter<*> {
        return when (entityType) {
            SyncConstants.EntityType.COURSE.value -> courseAdapter
            SyncConstants.EntityType.TABLE.value -> tableAdapter
            SyncConstants.EntityType.COURSE_NODE.value -> courseNodeAdapter
            SyncConstants.EntityType.ORDINARY_SCHEDULE.value -> ordinaryScheduleAdapter
            SyncConstants.EntityType.TIME_SLOT.value -> timeSlotAdapter
            else -> throw IllegalArgumentException("找不到类型 '$entityType' 的适配器")
        }
    }

    /**
     * 根据实体类型枚举获取对应的适配器
     *
     * @param entityType 实体类型枚举
     * @return 对应类型的适配器
     */
    fun getAdapter(entityType: SyncConstants.EntityType): SynkAdapter<*> {
        return getAdapter(entityType.value)
    }
} 