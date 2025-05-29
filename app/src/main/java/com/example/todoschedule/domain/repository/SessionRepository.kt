package com.example.todoschedule.domain.repository

import com.example.todoschedule.domain.model.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 用户会话管理仓库接口
 */
interface SessionRepository {
    /**
     * 当前登录用户的 ID Flow。
     * 如果没有用户登录，值为 null。
     */
    val currentUserIdFlow: StateFlow<Long?>

    /**
     * 保存登录用户的 ID。
     * @param userId 要保存的用户 ID。
     */
    suspend fun saveUserId(userId: Long)

    /**
     * 清除已保存的用户 ID (用于登出)。
     */
    suspend fun clearUserId()

    /**
     * 获取当前登录用户的ID。
     * @return 当前用户ID的Flow，如果未登录则为null
     */
    fun getUserId(): Flow<Long?>

    /**
     * 当前 UI 主题设置 Flow。
     */
    val themeSettingsFlow: StateFlow<ThemeSettings>

    /**
     * 更新深色模式设置。
     */
    suspend fun updateDarkTheme(isDark: Boolean)

    /**
     * 更新 Material You 设置。
     */
    suspend fun updateMaterialYou(useMaterialYou: Boolean)

    /**
     * 获取用户token
     */
    suspend fun getUserToken(): String?

    /**
     * 保存用户token
     */
    suspend fun saveUserToken(token: String)

    /**
     * 清除用户token
     */
    suspend fun clearUserToken()

    /**
     * 保存认证令牌
     */
    suspend fun saveAuthToken(token: String)

    /**
     * 获取认证令牌
     */
    fun getAuthToken(): Flow<String?>

    /**
     * 清除认证令牌
     */
    suspend fun clearAuthToken()

    /**
     * 清除所有会话数据
     */
    suspend fun clearAllSessionData()
} 