package com.example.todoschedule.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.todoschedule.data.database.entity.TableTimeConfigEntity
import com.example.todoschedule.data.database.entity.TableTimeConfigNodeDetaileEntity

/**
 * 用于 Room 查询的关联模型。
 * 包含一个课表时间配置及其所有关联的节点详情。
 */
data class TableTimeConfigWithNodes(
    @Embedded val config: TableTimeConfigEntity,
    @Relation(
        parentColumn = "id", // TableTimeConfigEntity 的主键
        entityColumn = "table_time_config_id" // TableTimeConfigNodeDetaileEntity 的外键
    )
    val nodes: List<TableTimeConfigNodeDetaileEntity> = emptyList()
) 