package com.example.todoschedule.domain.use_case.auth

import com.example.todoschedule.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * 清除用户登录会话 Use Case
 */
class ClearLoginSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke() {
        sessionRepository.clearUserId()
        sessionRepository.clearUserToken()
    }
} 