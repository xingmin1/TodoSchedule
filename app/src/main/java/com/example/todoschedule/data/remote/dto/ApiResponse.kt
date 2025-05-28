package com.example.todoschedule.data.remote.dto

/**
 * 通用API响应模型
 */
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
) 