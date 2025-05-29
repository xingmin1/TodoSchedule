package com.example.todoschedule.domain.repository

import com.example.todoschedule.domain.model.User
import kotlinx.coroutines.flow.Flow
import java.io.File

/** 用户仓库接口 */
interface UserRepository {
    /** 获取所有用户 */
    fun getAllUsers(): Flow<List<User>>

    /** 获取指定ID的用户 */
    suspend fun getUserById(id: Int): User?

    /** 添加用户 */
    suspend fun addUser(user: User): Long

    /** 注册新用户（包含密码哈希处理） */
    suspend fun registerUser(user: User): Result<Long>

    /** 根据用户名查找用户 */
    suspend fun findUserByUsername(username: String): User?

    /** 更新用户 */
    suspend fun updateUser(user: User)

    /** 初始化用户数据 如果没有用户，则创建一个默认用户 */
    suspend fun initDefaultUserIfNeeded(): Int

    /** 获取当前登录的用户 */
    suspend fun getCurrentUser(): User?

    suspend fun deleteUser(user: User)

    fun getInternalFilesDir(): File
}
