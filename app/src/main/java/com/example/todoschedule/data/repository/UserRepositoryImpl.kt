package com.example.todoschedule.data.repository

import android.content.Context
import android.util.Log
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.dao.UserDao
import com.example.todoschedule.data.mapper.toUser
import com.example.todoschedule.data.mapper.toUserEntity
import com.example.todoschedule.domain.model.User
import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** 用户仓库实现类 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val sessionRepository: SessionRepository,
    @ApplicationContext private val context: Context
) : UserRepository {

    override fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers().map { userEntities -> userEntities.map { it.toUser() } }
    }

    override suspend fun getUserById(userId: UUID): User? {
        return userDao.getUserById(userId)?.toUser()
    }

    override suspend fun addUser(user: User): Long {
        Log.d("UserRepository", "添加用户: ${user.username}")
        return userDao.insertUser(user.toUserEntity())
    }

    override suspend fun registerUser(user: User): Result<Long> {
        return try {
            val existingUser = userDao.getUserByUsername(user.username)
            if (existingUser != null) {
                Result.failure(Exception("用户名 '${user.username}' 已被注册"))
            } else {
                val userId = userDao.insertUser(user.toUserEntity())
                Result.success(userId)
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "注册用户失败: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun findUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)?.toUser()
    }

    override suspend fun updateUser(user: User) {
        userDao.updateUser(user.toUserEntity())
    }

    override suspend fun deleteUser(user: User) {
        userDao.deleteUser(user.id)
    }

    override fun getInternalFilesDir(): File {
        return context.filesDir
    }

    override suspend fun initDefaultUserIfNeeded(): Int {
        // 检查是否已有用户
        val userCount = userDao.getUserCount()
        if (userCount == 0) {
            // 创建默认用户
            val defaultUser = User(username = AppConstants.Database.DEFAULT_USER_NAME)
            val userId = addUser(defaultUser).toInt()
            Log.d("UserRepository", "创建了默认用户，ID: $userId")
            return userId
        }

        // 已有用户，直接返回第一个用户的ID
        Log.d(
            "UserRepositoryImpl",
            "已有用户，系统不再自动返回默认用户"
        )
        return AppConstants.Ids.INVALID_USER_ID.toInt()
    }

    /**
     * 获取当前登录的用户
     * @return 当前登录的用户，如果没有登录则返回null
     */
    override suspend fun getCurrentUser(): User? {
        val currentUserId = sessionRepository.currentUserIdFlow.first()
        if (currentUserId == null || currentUserId <= 0) {
            return null
        }
        return getUserById(currentUserId.toInt())
    }
}
