package com.example.todoschedule.data.sync.adapter

import com.example.todoschedule.data.database.converter.ReminderType
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.data.database.entity.TimeSlotEntity
import com.tap.synk.adapter.SynkAdapter as CoreSynkAdapter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 时间槽同步适配器
 */
@Singleton
class TimeSlotAdapter @Inject constructor() :
    AbstractSynkAdapter<TimeSlotEntity>(),
    CoreSynkAdapter<TimeSlotEntity> {

    override fun key(value: TimeSlotEntity): String {
        return value.crdtKey
    }

    override fun serialize(value: TimeSlotEntity): Map<String, Any?> {
        return mapOf(
            // 业务字段
            "startTime" to value.startTime,
            "endTime" to value.endTime,
            "scheduleType" to value.scheduleType.name,
            "scheduleId" to value.scheduleId,
            "head" to value.head,
            "priority" to value.priority,
            "isCompleted" to value.isCompleted,
            "isRepeated" to value.isRepeated,
            "repeatPattern" to value.repeatPattern,
            "reminderType" to value.reminderType?.name,
            "reminderOffset" to value.reminderOffset,

            // 同步字段
            "crdtKey" to value.crdtKey
        )
    }

    override fun deserialize(serialized: Map<String, Any?>): TimeSlotEntity {
        val scheduleTypeString = serialized["scheduleType"] as? String
        val scheduleType = if (scheduleTypeString != null) {
            try {
                ScheduleType.valueOf(scheduleTypeString)
            } catch (e: IllegalArgumentException) {
                ScheduleType.ORDINARY // 默认为普通日程
            }
        } else {
            ScheduleType.ORDINARY
        }

        val reminderTypeString = serialized["reminderType"] as? String
        val reminderType = if (reminderTypeString != null) {
            try {
                ReminderType.valueOf(reminderTypeString)
            } catch (e: IllegalArgumentException) {
                ReminderType.NONE
            }
        } else {
            ReminderType.NONE
        }

        return TimeSlotEntity(
            id = 0, // 本地ID在插入时由Room生成
            userId = 0, // 本地ID需要在数据库操作时通过外键关系设置
            startTime = getLong(serialized, "startTime", 0),
            endTime = getLong(serialized, "endTime", 0),
            scheduleType = scheduleType,
            scheduleId = getInt(serialized, "scheduleId", 0),
            head = serialized["head"] as? String,
            priority = serialized["priority"] as? Int,
            isCompleted = getBoolean(serialized, "isCompleted", false),
            isRepeated = getBoolean(serialized, "isRepeated", false),
            repeatPattern = serialized["repeatPattern"] as? String,
            reminderType = reminderType,
            reminderOffset = serialized["reminderOffset"] as? Long,

            // 同步字段
            crdtKey = getString(serialized, "crdtKey"),
            updateTimestamp = null // 在合并过程中设置
        )
    }

    override fun merge(local: TimeSlotEntity, remote: TimeSlotEntity): TimeSlotEntity {
        // 如果本地实体没有更新时间戳，或远程实体的时间戳更新，则使用远程实体
        if (local.updateTimestamp == null ||
            (remote.updateTimestamp != null && remote.updateTimestamp > local.updateTimestamp)
        ) {
            return remote.copy(
                id = local.id, // 保留本地ID
                userId = local.userId // 保留本地外键ID
            )
        }

        // 否则保留本地实体
        return local
    }
}
