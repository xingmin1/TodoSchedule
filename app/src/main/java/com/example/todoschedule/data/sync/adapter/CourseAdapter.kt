package com.example.todoschedule.data.sync.adapter

import com.example.todoschedule.data.database.entity.CourseEntity
import com.tap.hlc.Timestamp
import com.tap.synk.adapter.SynkAdapter as CoreSynkAdapter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 课程实体的SynkAdapter实现
 *
 * 负责课程实体的序列化、反序列化和冲突解决
 */
@Singleton
class CourseAdapter @Inject constructor() :
    AbstractSynkAdapter<CourseEntity>(),
    CoreSynkAdapter<CourseEntity> {

    override fun key(value: CourseEntity): String {
        return value.crdtKey
    }

    override fun serialize(value: CourseEntity): Map<String, Any?> {
        return mapOf(
            // 业务字段
            "courseName" to value.courseName,
            "color" to value.color,
            "room" to value.room,
            "teacher" to value.teacher,
            "startWeek" to value.startWeek,
            "endWeek" to value.endWeek,
            "credit" to value.credit,
            "courseCode" to value.courseCode,
            "syllabusLink" to value.syllabusLink,

            // 分布式引用关系
            "tableCrdtKey" to value.tableCrdtKey,

            // 同步字段
            "crdtKey" to value.crdtKey
        )
    }

    override fun deserialize(serialized: Map<String, Any?>): CourseEntity {
        return CourseEntity(
            id = 0, // 本地ID在插入时由Room生成
            tableId = 0, // 本地ID需要在数据库操作时通过tableCrdtKey查询得到
            courseName = getString(serialized, "courseName"),
            color = getString(serialized, "color", "#FF4081"),
            room = serialized["room"] as? String,
            teacher = serialized["teacher"] as? String,
            startWeek = getInt(serialized, "startWeek", 1),
            endWeek = getInt(serialized, "endWeek", 20),
            credit = (serialized["credit"] as? Number)?.toFloat(),
            courseCode = serialized["courseCode"] as? String,
            syllabusLink = serialized["syllabusLink"] as? String,

            // 分布式引用关系
            tableCrdtKey = serialized["tableCrdtKey"] as? String,

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

    override fun merge(local: CourseEntity, remote: CourseEntity): CourseEntity {
        // 如果本地实体没有更新时间戳，或远程实体的时间戳更新，则使用远程实体
        if (local.updateTimestamp == null ||
            (remote.updateTimestamp != null && remote.updateTimestamp > local.updateTimestamp)
        ) {
            return remote.copy(
                id = local.id, // 保留本地ID
                tableId = local.tableId // 保留本地外键ID
            )
        }

        // 否则保留本地实体
        return local
    }
} 
