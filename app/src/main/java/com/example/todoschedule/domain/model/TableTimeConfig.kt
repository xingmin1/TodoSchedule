package com.example.todoschedule.domain.model

/**
 * 课表时间配置的 Domain 模型。
 */
data class TableTimeConfig(
    val Id: UUID,
    val tableId: UUID, // 所属课表 ID
    val name: String, // 配置名称
    val isDefault: Boolean,
    val nodes: List<TableTimeConfigNode> // 包含的节点列表
) 