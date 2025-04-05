package com.example.todoschedule.domain.use_case.auth

import com.example.todoschedule.domain.model.User
import com.example.todoschedule.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 用户登录 Use Case
 */
class LoginUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val verifyPasswordUseCase: VerifyPasswordUseCase
) {
    suspend operator fun invoke(username: String, password: String): Result<User> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("用户名和密码不能为空"))
        }

        val user = userRepository.findUserByUsername(username)
            ?: return Result.failure(NoSuchElementException("用户不存在"))

        val isPasswordValid = verifyPasswordUseCase(password, user.passwordHash)

        return if (isPasswordValid) {
            Result.success(user)
        } else {
            Result.failure(IllegalArgumentException("密码错误"))
        }
    }
} 