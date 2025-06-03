package com.example.todoschedule.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todoschedule.data.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** 用户DAO接口 */
@Dao
interface UserDao {
    /** 获取所有用户 */
    @Query("SELECT * FROM user")
    fun getAllUsers(): Flow<List<UserEntity>>

    /** A获取指定ID的用户 */
    @Query("SELECT * FROM user WHERE id = :userId")
    suspend fun getUserById(userId: UUID): UserEntity?

    /** 根据用户名获取用户 */
    @Query("SELECT * FROM user WHERE username = :username")
    suspend fun getUserByUsername(username: String): UserEntity?

    /** 插入用户 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): UUID

    /** 更新用户 */
    @Update
    suspend fun updateUser(user: UserEntity)

    /** 获取用户数量 */
    @Query("SELECT COUNT(*) FROM user")
    suspend fun getUserCount(): Int

    /** 删除用户 */
    @Query("DELETE FROM user WHERE id = :userId")
    fun deleteUser(userId: UUID)
}