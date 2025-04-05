package com.example.todoschedule.data.repository

import android.util.Log
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.dao.UserDao
import com.example.todoschedule.data.mapper.toUser
import com.example.todoschedule.data.mapper.toUserEntity
import com.example.todoschedule.domain.model.User
import com.example.todoschedule.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** 用户仓库实现类 */
class UserRepositoryImpl @Inject constructor(private val userDao: UserDao) : UserRepository {

    override fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers().map { userEntities -> userEntities.map { it.toUser() } }
    }

    override suspend fun getUserById(userId: Int): User? {
        return userDao.getUserById(userId)?.toUser()
    }

    override suspend fun addUser(user: User): Long {
        Log.w("UserRepository", "调用了未处理密码哈希的 addUser 方法")
        return userDao.insertUser(user.toUserEntity())
    }

    override suspend fun registerUser(user: User): Result<Long> {
        return try {
            val existingUser = userDao.getUserByUsername(user.username)
            if (existingUser != null) {
                Result.failure(Exception("用户名 '${user.username}' 已被注册"))
            } else {
                user
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
        Log.w(
            "UserRepositoryImpl",
            "initDefaultUserIfNeeded 在登录系统存在时不应再依赖返回有效用户 ID，将返回 INVALID_USER_ID"
        )
        return AppConstants.Ids.INVALID_USER_ID.toInt()
    }
}
