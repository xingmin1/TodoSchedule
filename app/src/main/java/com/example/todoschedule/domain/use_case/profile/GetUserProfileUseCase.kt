package com.example.todoschedule.domain.use_case.profile

import com.example.todoschedule.domain.model.User
import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.repository.UserRepository
import com.example.todoschedule.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for getting the user profile.
 */
class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val userId = sessionRepository.currentUserIdFlow.value
                ?: throw Exception("未登录用户") // Or handle as a specific error type

            val user = userRepository.getUserById(userId)
                ?: throw Exception("未找到用户信息") // Or handle as a specific error type

            emit(Resource.Success(user))
        } catch (e: Exception) {
            // Consider logging the exception e
            emit(Resource.Error(e.message ?: "获取用户信息失败"))
        }
    }
} 