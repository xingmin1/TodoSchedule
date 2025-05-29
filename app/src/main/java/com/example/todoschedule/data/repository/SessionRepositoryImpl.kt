package com.example.todoschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.todoschedule.domain.model.ThemeSettings
import com.example.todoschedule.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户会话管理仓库实现类
 */
@Singleton // 标记为 Singleton，因为会话状态应该是全局唯一的
class SessionRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SessionRepository {

    // 使用 SupervisorJob + Dispatchers.IO 来创建独立的 CoroutineScope
    // 这样即使 DataStore 操作失败，也不会影响到注入此 Repository 的 ViewModel 的 Scope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private object PreferencesKeys {
        val USER_ID = longPreferencesKey("user_id")
        val USER_TOKEN = stringPreferencesKey("user_token")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val USE_MATERIAL_YOU = booleanPreferencesKey("use_material_you")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
    }

    /**
     * 当前登录用户的 ID Flow。
     * 如果没有用户登录，值为 null。
     */
    override val currentUserIdFlow: StateFlow<Long?> = dataStore.data
        .catch { exception ->
            // dataStore.data 在读取时可能会抛出 IOException
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.USER_ID]
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000), // 在最后一个订阅者消失后 5 秒停止共享
            initialValue = null // 初始值设为 null，表示未知或未登录
        )

    /**
     * 获取当前登录用户的ID。
     * @return 当前用户ID的Flow，如果未登录则为null
     */
    override fun getUserId(): Flow<Long?> = currentUserIdFlow

    override val themeSettingsFlow: StateFlow<ThemeSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            ThemeSettings(
                isDarkTheme = preferences[PreferencesKeys.IS_DARK_THEME] ?: false,
                useMaterialYou = preferences[PreferencesKeys.USE_MATERIAL_YOU]
                    ?: true // Material You 默认开启
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeSettings() // 使用默认值初始化
        )

    /**
     * 保存登录用户的 ID。
     * @param userId 要保存的用户 ID。
     */
    override suspend fun saveUserId(userId: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userId
        }
    }

    override suspend fun clearUserId() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USER_ID)
        }
    }

    override suspend fun updateDarkTheme(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_THEME] = isDark
        }
    }

    override suspend fun updateMaterialYou(useMaterialYou: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_MATERIAL_YOU] = useMaterialYou
        }
    }

    override suspend fun getUserToken(): String? {
        return dataStore.data.first()[PreferencesKeys.USER_TOKEN]
    }

    override suspend fun saveUserToken(token: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_TOKEN] = token
        }
    }

    override suspend fun clearUserToken() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USER_TOKEN)
        }
    }

    override suspend fun saveAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTH_TOKEN] = token
        }
    }

    override fun getAuthToken(): Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.AUTH_TOKEN]
        }

    override suspend fun clearAuthToken() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.AUTH_TOKEN)
        }
    }

    override suspend fun clearAllSessionData() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.AUTH_TOKEN)
            preferences.remove(PreferencesKeys.USER_ID)
            preferences.remove(PreferencesKeys.USER_TOKEN)
            // 如果有其他与会话相关的数据也在这里清除
        }
    }
} 