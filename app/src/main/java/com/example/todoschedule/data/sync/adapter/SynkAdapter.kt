package com.example.todoschedule.data.sync.adapter

/**
 * SynkAdapter接口
 *
 * Synk库需要的适配器接口，用于实现实体与CRDT数据之间的转换
 * 每种需要同步的实体类型都需要实现一个对应的适配器
 *
 * @param T 实体类型
 */
interface SynkAdapter<T> {
    /**
     * 提取实体的唯一标识符
     *
     * @param value 实体对象
     * @return 实体的全局唯一标识符
     */
    fun key(value: T): String

    /**
     * 序列化实体为Map结构
     *
     * @param value 实体对象
     * @return 包含实体所有字段的Map
     */
    fun serialize(value: T): Map<String, Any?>

    /**
     * 反序列化Map结构为实体
     *
     * @param serialized 序列化后的Map结构
     * @return 实体对象
     */
    fun deserialize(serialized: Map<String, Any?>): T

    /**
     * 合并本地和远程实体，解决冲突
     *
     * @param local 本地实体
     * @param remote 远程实体
     * @return 合并后的实体
     */
    fun merge(local: T, remote: T): T
}

/**
 * SynkAdapter抽象基类
 *
 * 为所有适配器提供通用功能的基类实现
 *
 * @param T 实体类型
 */
abstract class AbstractSynkAdapter<T> : SynkAdapter<T> {

    /**
     * 检查是否需要合并
     *
     * 如果远程版本更新，则需要合并
     *
     * @param localTime 本地时间戳
     * @param remoteTime 远程时间戳
     * @return 是否需要合并
     */
    protected fun shouldMerge(localTime: Long, remoteTime: Long): Boolean {
        return remoteTime > localTime
    }

    /**
     * 从序列化的Map中获取Long类型的值
     *
     * @param map 序列化的Map
     * @param key 键
     * @param defaultValue 默认值
     * @return Long值
     */
    protected fun getLong(map: Map<String, Any?>, key: String, defaultValue: Long = 0L): Long {
        val value = map[key]
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: defaultValue
            else -> defaultValue
        }
    }

    /**
     * 从序列化的Map中获取Int类型的值
     *
     * @param map 序列化的Map
     * @param key 键
     * @param defaultValue 默认值
     * @return Int值
     */
    protected fun getInt(map: Map<String, Any?>, key: String, defaultValue: Int = 0): Int {
        val value = map[key]
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: defaultValue
            else -> defaultValue
        }
    }

    /**
     * 从序列化的Map中获取Boolean类型的值
     *
     * @param map 序列化的Map
     * @param key 键
     * @param defaultValue 默认值
     * @return Boolean值
     */
    protected fun getBoolean(
        map: Map<String, Any?>,
        key: String,
        defaultValue: Boolean = false
    ): Boolean {
        val value = map[key]
        return when (value) {
            is Boolean -> value
            is String -> value.toBoolean()
            is Number -> value.toInt() != 0
            else -> defaultValue
        }
    }

    /**
     * 从序列化的Map中获取String类型的值
     *
     * @param map 序列化的Map
     * @param key 键
     * @param defaultValue 默认值
     * @return String值
     */
    protected fun getString(
        map: Map<String, Any?>,
        key: String,
        defaultValue: String = ""
    ): String {
        val value = map[key]
        return when (value) {
            is String -> value
            null -> defaultValue
            else -> value.toString()
        }
    }
} 