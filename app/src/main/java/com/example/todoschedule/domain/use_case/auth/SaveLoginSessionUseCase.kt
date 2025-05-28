package com.example.todoschedule.domain.use_case.auth

import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 保存用户登录会话 (用户 ID 和 token) Use Case
 */
class SaveLoginSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository
) {
    /**
     * 保存用户登录会话
     * @param userId 用户ID
     * @param token 用户token，如果为null则从数据库获取
     */
    suspend operator fun invoke(userId: Long, token: String? = null) {
        // 保存用户ID
        sessionRepository.saveUserId(userId)

        // 保存token
        if (token != null) {
            // 如果提供了token，直接保存
            sessionRepository.saveUserToken(token)
            // 同时保存为认证令牌
            sessionRepository.saveAuthToken(token)
        } else {
            // 如果没有提供token，从数据库获取
            val user = userRepository.getUserById(userId.toInt())
            if (user?.token != null) {
                sessionRepository.saveUserToken(user.token)
                // 同时保存为认证令牌
                sessionRepository.saveAuthToken(user.token)
            }
        }
    }
} 