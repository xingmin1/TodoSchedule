package com.example.todoschedule.data.sync.adapter

import com.example.todoschedule.data.database.entity.TableEntity
import com.tap.synk.adapter.SynkAdapter as CoreSynkAdapter
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 课表实体的SynkAdapter实现
 */
@Singleton
class TableAdapter @Inject constructor() :
    AbstractSynkAdapter<TableEntity>(),
    CoreSynkAdapter<TableEntity> {

    override fun key(value: TableEntity): String {
        return value.crdtKey
    }

    override fun serialize(value: TableEntity): Map<String, Any?> {
        return mapOf(
            // 业务字段
            "tableName" to value.tableName,
            "background" to value.background,
            "listPosition" to value.listPosition,
            "terms" to value.terms,
            "startDate" to value.startDate.toString(),
            "totalWeeks" to value.totalWeeks,

            // 分布式引用关系
            "userCrdtKey" to value.userCrdtKey,

            // 同步字段
            "crdtKey" to value.crdtKey
        )
    }

    override fun deserialize(serialized: Map<String, Any?>): TableEntity {
        return TableEntity(
            id = 0, // 本地ID在插入时由Room生成
            userId = 0, // 本地ID需要在数据库操作时通过userCrdtKey查询得到
            tableName = getString(serialized, "tableName"),
            background = getString(serialized, "background", ""),
            listPosition = getInt(serialized, "listPosition", 0),
            terms = getString(serialized, "terms", ""),
            startDate = LocalDate.parse(getString(serialized, "startDate")),
            totalWeeks = getInt(serialized, "totalWeeks", 20),

            // 分布式引用关系
            userCrdtKey = serialized["userCrdtKey"] as? String,

            // 同步字段
            crdtKey = getString(serialized, "crdtKey"),
            updateTimestamp = null // 在合并过程中设置
        )
    }

    /* Synk-Adapter 接口映射 */
    override fun resolveId(crdt: TableEntity): String = key(crdt)

    override fun encode(crdt: TableEntity): Map<String, String> =
        serialize(crdt).mapValues { it.value?.toString() ?: "" }

    override fun decode(map: Map<String, String>): TableEntity =
        deserialize(map as Map<String, Any?>)

    override fun merge(local: TableEntity, remote: TableEntity): TableEntity {
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
