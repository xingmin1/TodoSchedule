package com.example.todoschedule.domain.use_case.auth

import com.example.todoschedule.domain.model.User
import com.example.todoschedule.domain.repository.RemoteUserRepository
import com.example.todoschedule.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 用户登录 Use Case，支持本地和远程登录
 */
class LoginUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val remoteUserRepository: RemoteUserRepository,
    private val verifyPasswordUseCase: VerifyPasswordUseCase
) {
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @param useRemote 是否使用远程登录，默认为true
     * @return 登录结果
     */
    suspend operator fun invoke(
        username: String,
        password: String,
        useRemote: Boolean = true
    ): Result<User> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("用户名和密码不能为空"))
        }

        // 优先尝试远程登录
        if (useRemote) {
            try {
                // 直接使用明文密码进行远程登录
                val remoteResult = remoteUserRepository.login(username, password)
                if (remoteResult.isSuccess) {
                    // 远程登录成功，将用户信息保存到本地
                    val remoteUser = remoteResult.getOrNull()!!

                    // 查询本地是否已有该用户
                    val localUser = userRepository.findUserByUsername(username)

                    if (localUser != null) {
                        // 本地已有该用户，更新信息
                        val updatedUser = localUser.copy(
                            token = remoteUser.token,
                            lastOpen = remoteUser.lastOpen
                        )
                        userRepository.updateUser(updatedUser)
                        return Result.success(updatedUser)
                    } else {
                        // 本地没有该用户，创建新用户
                        val userId = userRepository.addUser(remoteUser)
                        return Result.success(remoteUser.copy(id = userId))
                    }
                }
                // 如果远程登录失败，但不是网络错误等严重问题，可以尝试本地登录
            } catch (e: Exception) {
                // 远程登录出现异常，尝试本地登录
            }
        }

        // 尝试本地登录
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