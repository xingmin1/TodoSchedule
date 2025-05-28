package com.example.todoschedule.data.sync.dto

import kotlinx.serialization.Serializable

/**
 * API标准响应格式
 * 服务器API响应的标准格式
 * @param code 状态码
 * @param message 响应消息
 * @param data 数据内容，可能为null
 */
@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null
)
