package com.example.todoschedule.data.remote.model

/**
 * API 响应封装类
 */
data class ApiResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null,
    val success: Boolean = code == 0
) 