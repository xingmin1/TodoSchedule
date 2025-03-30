package com.example.todoschedule.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todoschedule.data.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/** 用户DAO接口 */
@Dao
interface UserDao {
    /** 获取所有用户 */
    @Query("SELECT * FROM user")
    fun getAllUsers(): Flow<List<UserEntity>>

    /** A获取指定ID的用户 */
    @Query("SELECT * FROM user WHERE id = :userId")
    suspend fun getUserById(userId: Int): UserEntity?

    /** 获取当前活跃用户（目前简单实现，取第一个用户） */
    @Query("SELECT * FROM user ORDER BY id LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    /** 插入用户 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    /** 更新用户 */
    @Update
    suspend fun updateUser(user: UserEntity)

    /** 获取用户数量 */
    @Query("SELECT COUNT(*) FROM user")
    suspend fun getUserCount(): Int
}
