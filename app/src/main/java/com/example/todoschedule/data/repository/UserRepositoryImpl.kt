package com.example.todoschedule.data.repository

import android.util.Log
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.dao.UserDao
import com.example.todoschedule.data.mapper.toUser
import com.example.todoschedule.data.mapper.toUserEntity
import com.example.todoschedule.domain.model.User
import com.example.todoschedule.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    override fun getCurrentUser(): Flow<User?> {
        return userDao.getCurrentUser().map { it?.toUser() }
    }

    override suspend fun addUser(user: User): Long {
        return userDao.insertUser(user.toUserEntity())
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
        try {
            val user = userDao.getCurrentUser().first()
            val userId = user?.id ?: AppConstants.Ids.INVALID_USER_ID
            if (userId == AppConstants.Ids.INVALID_USER_ID) {
                Log.w("UserRepository", "获取当前用户ID失败")
            }
            return userId
        } catch (e: Exception) {
            Log.e("UserRepository", "获取当前用户异常: ${e.message}")
            return AppConstants.Ids.INVALID_USER_ID
        }
    }
}
