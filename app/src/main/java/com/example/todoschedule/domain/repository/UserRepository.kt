package com.example.todoschedule.domain.repository

import com.example.todoschedule.domain.model.User
import kotlinx.coroutines.flow.Flow

/** 用户仓库接口 */
interface UserRepository {
    /** 获取所有用户 */
    fun getAllUsers(): Flow<List<User>>

    /** 获取指定ID的用户 */
    suspend fun getUserById(userId: Int): User?

    /** 获取当前活跃用户 */
    fun getCurrentUser(): Flow<User?>

    /** 添加用户 */
    suspend fun addUser(user: User): Long

    /** 更新用户 */
    suspend fun updateUser(user: User)

    /** 初始化用户数据 如果没有用户，则创建一个默认用户 */
    suspend fun initDefaultUserIfNeeded(): Int
}
