package com.example.todoschedule.data.sync

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设备ID管理器
 *
 * 负责生成和管理设备的唯一标识符，这在CRDT系统中至关重要，
 * 因为设备ID被用作混合逻辑时钟(HLC)的节点标识符。
 */
@Singleton
class DeviceIdManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val DEVICE_ID_PREFERENCES = "device_id_preferences"
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")

        // 创建DataStore实例
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = DEVICE_ID_PREFERENCES
        )
    }

    private val TAG = "DeviceIdManager"

    // 缓存的设备ID，避免重复读取DataStore
    private var cachedDeviceId: String? = null

    // 互斥锁，确保设备ID的线程安全访问
    private val mutex = Mutex()

    /**
     * 获取设备ID，如果不存在则创建一个新的并保存
     *
     * 设备ID在应用程序的生命周期内必须保持不变，因为它被用作
     * CRDT系统中的节点标识符，用于跟踪数据来源和解决冲突。
     *
     * @return 当前设备的唯一标识符
     */
    suspend fun getOrCreateDeviceId(): String {
        return mutex.withLock {
            Log.d(TAG, "获取或创建设备ID")
            // 如果已有缓存，直接返回
            cachedDeviceId?.let { return@withLock it }

            val storedId = context.dataStore.data.map { preferences ->
                preferences[DEVICE_ID_KEY]
            }.first()

            if (storedId != null) {
                Log.d(TAG, "使用已存在的设备ID: $storedId")
                cachedDeviceId = storedId
                return@withLock storedId
            }

            // 生成新的设备ID
            val newDeviceId = generateDeviceId()
            Log.d(TAG, "生成新的设备ID: $newDeviceId")

            // 保存到DataStore
            context.dataStore.edit { preferences ->
                preferences[DEVICE_ID_KEY] = newDeviceId
            }

            cachedDeviceId = newDeviceId
            return@withLock newDeviceId
        }
    }

    /**
     * 获取缓存的设备ID
     *
     * 仅返回内存中缓存的设备ID，不从存储中读取
     * @return 缓存的设备ID，如果未初始化则抛出异常
     * @throws IllegalStateException 如果设备ID尚未初始化
     */
    fun getCachedDeviceId(): String {
        return cachedDeviceId
            ?: throw IllegalStateException("设备ID尚未初始化，请先调用getOrCreateDeviceId()")
    }

    /**
     * 生成新的设备ID
     *
     * @return 新生成的UUID
     */
    private fun generateDeviceId(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * 检查设备ID是否已存在
     *
     * @return 如果设备ID已存在则返回true
     */
    suspend fun hasDeviceId(): Boolean {
        return try {
            val preferences = context.dataStore.data.first()
            preferences.contains(DEVICE_ID_KEY)
        } catch (e: Exception) {
            false
        }
    }
} 