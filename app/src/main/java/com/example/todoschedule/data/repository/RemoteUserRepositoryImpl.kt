package com.example.todoschedule.data.repository

import android.util.Log
import com.example.todoschedule.data.remote.api.UserApiService
import com.example.todoschedule.data.remote.dto.UserLoginRequest
import com.example.todoschedule.data.remote.dto.UserRegisterRequest
import com.example.todoschedule.domain.model.User
import com.example.todoschedule.domain.repository.RemoteUserRepository
import com.example.todoschedule.domain.repository.SessionRepository
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 远程用户仓库实现类
 */
@Singleton
class RemoteUserRepositoryImpl @Inject constructor(
    private val userApiService: UserApiService,
    private val sessionRepository: SessionRepository
) : RemoteUserRepository {

    override suspend fun login(username: String, password: String): Result<User> {
        return try {
            // 使用明文密码，虽然字段名为passwordHash，但传递的是明文密码
            // 密码哈希由服务器端处理
            val request = UserLoginRequest(username, password = password)
            val response = userApiService.login(request)

            if (response.code == 200 && response.data != null) {
                // 登录成功，保存token
                val userData = response.data
                sessionRepository.saveUserToken(userData.token)

                // 转换为领域模型
                val user = User(
                    id = userData.id,
                    username = userData.username,
                    token = userData.token,
                    lastOpen = if (userData.last_open != null) {
                        try {
                            // 尝试解析时间字符串，实际实现可能需要更复杂的逻辑
                            Instant.parse(userData.last_open)
                        } catch (e: Exception) {
                            Instant.parse("2023-01-01T00:00:00Z") // 默认时间
                        }
                    } else {
                        Instant.parse("2023-01-01T00:00:00Z") // 默认时间
                    }
                )
                Result.success(user)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Log.e("RemoteUserRepo", "登录失败: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun register(
        username: String,
        password: String,
        phoneNumber: String?,
        email: String?
    ): Result<User> {
        return try {
            // 使用明文密码，虽然字段名为passwordHash，但传递的是明文密码
            // 密码哈希由服务器端处理
            val request = UserRegisterRequest(
                username = username,
                passwordHash = password,
                phone_number = phoneNumber,
                email = email
            )
            val response = userApiService.register(request)

            if (response.code == 200 && response.data != null) {
                // 注册成功，保存token
                val userData = response.data
                sessionRepository.saveUserToken(userData.token)

                // 转换为领域模型
                val user = User(
                    id = userData.id,
                    username = userData.username,
                    token = userData.token,
                    createdAt = if (userData.created_at != null) {
                        try {
                            Instant.parse(userData.created_at)
                        } catch (e: Exception) {
                            Instant.parse("2023-01-01T00:00:00Z")
                        }
                    } else {
                        Instant.parse("2023-01-01T00:00:00Z")
                    }
                )
                Result.success(user)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Log.e("RemoteUserRepo", "注册失败: ${e.message}")
            Result.failure(e)
        }
    }
} 