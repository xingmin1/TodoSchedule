package com.example.todoschedule.data.sync.adapter

import com.example.todoschedule.data.database.converter.ScheduleStatus
import com.example.todoschedule.data.database.entity.OrdinaryScheduleEntity
import com.tap.hlc.Timestamp
import com.tap.synk.adapter.SynkAdapter as CoreSynkAdapter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 普通日程实体的SynkAdapter实现
 */
@Singleton
class OrdinaryScheduleAdapter @Inject constructor() :
    AbstractSynkAdapter<OrdinaryScheduleEntity>(),
    CoreSynkAdapter<OrdinaryScheduleEntity> {

    override fun key(value: OrdinaryScheduleEntity): String {
        return value.crdtKey
    }

    override fun serialize(value: OrdinaryScheduleEntity): Map<String, Any?> {
        return mapOf(
            // 业务字段
            "title" to value.title,
            "description" to value.description,
            "location" to value.location,
            "category" to value.category,
            "color" to value.color,
            "isAllDay" to value.isAllDay,
            "status" to value.status?.name,

            // 分布式引用关系
            "userCrdtKey" to value.userCrdtKey,

            // 同步字段
            "crdtKey" to value.crdtKey
        )
    }

    override fun deserialize(serialized: Map<String, Any?>): OrdinaryScheduleEntity {
        val statusString = serialized["status"] as? String
        val status = if (statusString != null) {
            try {
                ScheduleStatus.valueOf(statusString)
            } catch (e: IllegalArgumentException) {
                ScheduleStatus.TODO
            }
        } else {
            ScheduleStatus.TODO
        }

        return OrdinaryScheduleEntity(
            id = 0, // 本地ID在插入时由Room生成
            userId = 0, // 本地ID需要在数据库操作时通过userCrdtKey查询得到
            title = getString(serialized, "title"),
            description = serialized["description"] as? String,
            location = serialized["location"] as? String,
            category = serialized["category"] as? String,
            color = serialized["color"] as? String,
            isAllDay = getBoolean(serialized, "isAllDay", false),
            status = status,

            // 分布式引用关系
            userCrdtKey = serialized["userCrdtKey"] as? String,

            // 同步字段
            crdtKey = getString(serialized, "crdtKey"),
            updateTimestamp = null // 在合并过程中设置
        )
    }

    /* Synk-Adapter 接口映射 */
    override fun resolveId(crdt: CourseEntity): String = key(crdt)

    override fun encode(crdt: CourseEntity): Map<String, String> =
        serialize(crdt).mapValues { it.value?.toString() ?: "" }

    override fun decode(map: Map<String, String>): CourseEntity =
        deserialize(map as Map<String, Any?>)

    override fun merge(
        local: OrdinaryScheduleEntity,
        remote: OrdinaryScheduleEntity
    ): OrdinaryScheduleEntity {
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
