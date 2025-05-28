package com.example.todoschedule.util

/**
 * 一个通用的类，用于封装网络请求或其他操作的结果。
 * @param T 成功时数据的类型。
 * @property data 操作成功时返回的数据，可能为null。
 * @property message 操作失败或附带的信息，可能为null。
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    /**
     * 表示操作成功的状态。
     * @param data 成功返回的数据。
     */
    class Success<T>(data: T) : Resource<T>(data)

    /**
     * 表示操作失败的状态。
     * @param message 错误信息。
     * @param data 可选的附带数据。
     */
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)

    /**
     * 表示操作正在进行中的状态 (可选)。
     * @param data 可选的当前数据 (例如，在加载更多内容时)。
     */
    class Loading<T>(data: T? = null) : Resource<T>(data) // 可选，用于UI显示加载状态
} 