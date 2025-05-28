package com.example.todoschedule.domain.repository

import com.example.todoschedule.domain.model.User

/**
 * 远程用户仓库接口，处理与服务器的用户相关操作
 */
interface RemoteUserRepository {
    /**
     * 用户登录
     * @param username 用户名
     * @param password 明文密码（将通过API发送给服务器，服务器负责密码哈希处理）
     * @return 登录成功的用户信息
     */
    suspend fun login(username: String, password: String): Result<User>

    /**
     * 用户注册
     * @param username 用户名
     * @param password 明文密码（将通过API发送给服务器，服务器负责密码哈希处理）
     * @param phoneNumber 手机号
     * @param email 邮箱
     * @return 注册成功的用户信息
     */
    suspend fun register(
        username: String,
        password: String,
        phoneNumber: String? = null,
        email: String? = null
    ): Result<User>
} 