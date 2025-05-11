package com.example.todoschedule.domain.use_case.profile

import com.example.todoschedule.domain.use_case.auth.ClearLoginSessionUseCase
import javax.inject.Inject

/**
 * Use case for logging out the user.
 */
class LogoutUseCase @Inject constructor(
    private val clearLoginSessionUseCase: ClearLoginSessionUseCase
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            clearLoginSessionUseCase()
            Result.success(Unit)
        } catch (e: Exception) {
            // Consider logging the exception e
            Result.failure(e)
        }
    }
} 