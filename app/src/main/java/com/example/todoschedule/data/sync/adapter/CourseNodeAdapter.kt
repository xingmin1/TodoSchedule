package com.example.todoschedule.data.sync.adapter

import com.example.todoschedule.data.database.entity.CourseNodeEntity
import com.tap.hlc.Timestamp
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 课程节点实体的SynkAdapter实现
 */
@Singleton
class CourseNodeAdapter @Inject constructor() : AbstractSynkAdapter<CourseNodeEntity>() {

    override fun key(value: CourseNodeEntity): String {
        return value.crdtKey
    }

    override fun serialize(value: CourseNodeEntity): Map<String, Any?> {
        return mapOf(
            // 业务字段
            "courseNodeName" to value.courseNodeName,
            "color" to value.color,
            "room" to value.room,
            "teacher" to value.teacher,
            "startNode" to value.startNode,
            "step" to value.step,
            "day" to value.day,
            "startWeek" to value.startWeek,
            "endWeek" to value.endWeek,
            "weekType" to value.weekType,

            // 分布式引用关系
            "courseCrdtKey" to value.courseCrdtKey,

            // 同步字段
            "crdtKey" to value.crdtKey
        )
    }

    override fun deserialize(serialized: Map<String, Any?>): CourseNodeEntity {
        return CourseNodeEntity(
            id = 0, // 本地ID在插入时由Room生成
            courseId = 0, // 本地ID需要在数据库操作时通过courseCrdtKey查询得到
            courseNodeName = serialized["courseNodeName"] as? String,
            color = serialized["color"] as? String,
            room = serialized["room"] as? String,
            teacher = serialized["teacher"] as? String,
            startNode = getInt(serialized, "startNode"),
            step = getInt(serialized, "step"),
            day = getInt(serialized, "day"),
            startWeek = getInt(serialized, "startWeek"),
            endWeek = getInt(serialized, "endWeek"),
            weekType = getInt(serialized, "weekType", 0),

            // 分布式引用关系
            courseCrdtKey = serialized["courseCrdtKey"] as? String,

            // 同步字段
            crdtKey = getString(serialized, "crdtKey"),
            updateTimestamp = null // 在合并过程中设置
        )
    }

    override fun merge(local: CourseNodeEntity, remote: CourseNodeEntity): CourseNodeEntity {
        // 如果本地实体没有更新时间戳，或远程实体的时间戳更新，则使用远程实体
        if (local.updateTimestamp == null ||
            (remote.updateTimestamp != null && remote.updateTimestamp > local.updateTimestamp)
        ) {
            return remote.copy(
                id = local.id, // 保留本地ID
                courseId = local.courseId // 保留本地外键ID
            )
        }

        // 否则保留本地实体
        return local
    }
} 