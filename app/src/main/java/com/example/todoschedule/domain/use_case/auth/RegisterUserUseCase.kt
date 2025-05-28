package com.example.todoschedule.domain.use_case.auth

import com.example.todoschedule.domain.model.User
import com.example.todoschedule.domain.repository.RemoteUserRepository
import com.example.todoschedule.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 注册用户 Use Case，支持本地和远程注册
 */
class RegisterUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val remoteUserRepository: RemoteUserRepository,
    private val hashPasswordUseCase: HashPasswordUseCase
) {
    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @param phoneNumber 手机号
     * @param email 邮箱
     * @param useRemote 是否使用远程注册，默认为true
     * @return 注册结果
     */
    suspend operator fun invoke(
        username: String,
        password: String,
        phoneNumber: String? = null,
        email: String? = null,
        useRemote: Boolean = true
    ): Result<Long> {
        // 验证输入
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("用户名和密码不能为空"))
        }

        // 优先尝试远程注册
        if (useRemote) {
            try {
                // 直接使用明文密码进行远程注册
                val remoteResult = remoteUserRepository.register(
                    username = username,
                    password = password, // 使用明文密码
                    phoneNumber = phoneNumber,
                    email = email
                )

                if (remoteResult.isSuccess) {
                    // 远程注册成功，将用户信息保存到本地
                    val remoteUser = remoteResult.getOrNull()!!

                    // 查询本地是否已有该用户
                    val localUser = userRepository.findUserByUsername(username)

                    if (localUser != null) {
                        // 本地已有该用户，应该是异常情况，因为前面远程注册成功意味着用户是新创建的
                        // 更新信息比较安全
                        val updatedUser = localUser.copy(
                            token = remoteUser.token,
                            passwordHash = remoteUser.passwordHash ?: hashPasswordUseCase(password)
                        )
                        userRepository.updateUser(updatedUser)
                        return Result.success(localUser.id.toLong())
                    } else {
                        // 本地没有该用户，创建新用户
                        // 如果远程用户没有密码哈希(正常情况)，本地创建密码哈希
                        val userWithHash = if (remoteUser.passwordHash == null) {
                            remoteUser.copy(passwordHash = hashPasswordUseCase(password))
                        } else {
                            remoteUser
                        }
                        val userId = userRepository.addUser(userWithHash)
                        return Result.success(userId)
                    }
                }
                // 如果远程注册失败，考虑本地注册，但请注意，这种情况下通常应该尊重服务器的决定
                // 例如，如果服务器说用户名已存在，我们不应该在本地创建
                val error = Exception(
                    "远程注册失败: " + (remoteResult.exceptionOrNull()?.message ?: "未知错误")
                )
                return Result.failure(error)
            } catch (e: Exception) {
                // 远程注册失败，可能是网络问题，尝试本地注册
                // 但现实中通常不建议这样做，因为可能导致用户以为注册成功，但其实没有在服务器注册成功
                // 此处仅提供逻辑示范
            }
        }

        // 如果不使用远程注册或远程注册失败后的备用方案
        // 本地密码哈希
        val passwordHash = hashPasswordUseCase(password)
        val newUser = User(
            username = username,
            passwordHash = passwordHash,
            phoneNumber = phoneNumber,
            email = email
        )

        // 调用 UserRepository 的 registerUser 方法
        return userRepository.registerUser(newUser)
    }
} 