package com.example.todoschedule.domain.use_case.auth

import com.example.todoschedule.domain.repository.SessionRepository
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import javax.inject.Inject

/**
 * 获取当前登录用户 ID 的 Flow Use Case
 */
class GetLoginUserIdFlowUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(): StateFlow<UUID?> {
        return sessionRepository.currentUserIdFlow
    }
} 