package com.example.todoschedule.data.remote.dto

/**
 * 用户相关的数据传输对象(DTOs)
 *
 * 架构决策说明：
 * 为了提高安全性，我们将密码的哈希处理移至服务器端进行，而不是在客户端。
 * 尽管API的请求字段名仍为"passwordHash"，但实际上我们会发送明文密码。
 * 服务器接收到密码后负责进行安全的哈希处理。
 */

/**
 * 用户登录请求
 */
data class UserLoginRequest(
    val username: String,
    /**
     * 明文密码，由服务器端负责哈希处理
     */
    val password: String
)

/**
 * 用户注册请求
 */
data class UserRegisterRequest(
    val username: String,
    /**
     * 尽管字段名为passwordHash，但实际传递的是明文密码
     * 密码的哈希处理由服务器端负责
     */
    val passwordHash: String,
    val phone_number: String? = null,
    val email: String? = null
)

/**
 * 用户响应数据
 */
data class UserDto(
    val id: Int,
    val username: String,
    val token: String,
    val created_at: String? = null,
    val last_open: String? = null
) 