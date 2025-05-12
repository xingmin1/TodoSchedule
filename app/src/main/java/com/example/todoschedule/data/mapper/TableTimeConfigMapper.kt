package com.example.todoschedule.data.mapper

import com.example.todoschedule.data.database.entity.TableTimeConfigEntity
import com.example.todoschedule.data.database.entity.TableTimeConfigNodeDetaileEntity
import com.example.todoschedule.data.model.TableTimeConfigWithNodes
import com.example.todoschedule.domain.model.TableTimeConfig
import com.example.todoschedule.domain.model.TableTimeConfigNode

/**
 * 将 TableTimeConfigNodeDetaileEntity (Data) 转换为 TableTimeConfigNode (Domain)。
 */
fun TableTimeConfigNodeDetaileEntity.toDomainModel(): TableTimeConfigNode {
    return TableTimeConfigNode(
        id = this.id,
        name = this.name,
        startTime = this.startTime,
        endTime = this.endTime,
        node = this.node
    )
}

/**
 * 将 TableTimeConfigWithNodes (Data, 包含关联) 转换为 TableTimeConfig (Domain)。
 */
fun TableTimeConfigWithNodes.toDomainModel(): TableTimeConfig {
    return TableTimeConfig(
        id = this.config.id,
        tableId = this.config.tableId,
        name = this.config.name,
        isDefault = this.config.isDefault,
        nodes = this.nodes.map { it.toDomainModel() } // 映射子节点列表
    )
}

// 可以根据需要添加从 Domain Model 到 Entity 的反向映射

/**
 * 将 TableTimeConfigNode (Domain) 转换为 TableTimeConfigNodeDetaileEntity (Data)。
 */
fun TableTimeConfig.toTableTimeConfigWithNodes(): TableTimeConfigWithNodes {
    return TableTimeConfigWithNodes(
        config = TableTimeConfigEntity(
            id = this.id,
            tableId = this.tableId,
            name = this.name,
            isDefault = this.isDefault
        ),
        nodes = this.nodes.map { node ->
            TableTimeConfigNodeDetaileEntity(
                id = node.id,
                name = node.name,
                startTime = node.startTime,
                endTime = node.endTime,
                node = node.node,
                tableTimeConfigId = this.id
            )
        }
    )
}