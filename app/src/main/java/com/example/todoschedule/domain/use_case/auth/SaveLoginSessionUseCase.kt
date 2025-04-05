package com.example.todoschedule.domain.use_case.auth

import com.example.todoschedule.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * 保存用户登录会话 (用户 ID) Use Case
 */
class SaveLoginSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(userId: Long) {
        sessionRepository.saveUserId(userId)
    }
} 