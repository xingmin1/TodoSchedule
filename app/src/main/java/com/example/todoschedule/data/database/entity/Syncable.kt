package com.example.todoschedule.data.database.entity

/**
 * 可同步接口
 * 所有需要同步的实体类都需要实现该接口
 */
interface Syncable {
    /**
     * 获取实体的同步ID（通常是crdtKey）
     * 用于在分布式系统中唯一标识该实体
     */
    val syncId: String
} 