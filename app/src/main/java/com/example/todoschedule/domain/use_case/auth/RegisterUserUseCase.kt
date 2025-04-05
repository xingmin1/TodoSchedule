package com.example.todoschedule.domain.use_case.auth

import com.example.todoschedule.domain.model.User
import com.example.todoschedule.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 注册用户 Use Case
 */
class RegisterUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val hashPasswordUseCase: HashPasswordUseCase
) {
    suspend operator fun invoke(username: String, password: String): Result<Long> {
        // 可以在这里添加更复杂的验证逻辑，例如密码强度检查
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("用户名和密码不能为空"))
        }

        val passwordHash = hashPasswordUseCase(password)
        val newUser = User(username = username, passwordHash = passwordHash)

        // 调用 UserRepository 的 registerUser 方法
        return userRepository.registerUser(newUser)
    }
} 