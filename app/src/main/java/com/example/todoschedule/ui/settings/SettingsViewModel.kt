package com.example.todoschedule.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.core.utils.DevUtils
import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.use_case.auth.ClearLoginSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页面ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val clearLoginSessionUseCase: ClearLoginSessionUseCase,
    private val sessionRepository: SessionRepository
) : AndroidViewModel(application) {

    // 注入DevUtils
    @Inject
    lateinit var devUtils: DevUtils

    // 深色模式 - 从 SessionRepository 获取初始值和更新
    private val _darkTheme = MutableStateFlow(false)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    // 是否使用Material You - 从 SessionRepository 获取初始值和更新
    private val _materialYou = MutableStateFlow(true)
    val materialYou: StateFlow<Boolean> = _materialYou.asStateFlow()

    // 一周的第一天（1:周一，7:周日）
    private val _firstDayOfWeek = MutableStateFlow(1)
    val firstDayOfWeek: StateFlow<Int> = _firstDayOfWeek.asStateFlow()

    // 数据库操作状态
    private val _databaseOperation = MutableStateFlow<DatabaseOperation>(DatabaseOperation.Idle)
    val databaseOperation: StateFlow<DatabaseOperation> = _databaseOperation.asStateFlow()

    init {
        // 观察主题设置变化
        sessionRepository.themeSettingsFlow
            .onEach { settings ->
                _darkTheme.value = settings.isDarkTheme
                _materialYou.value = settings.useMaterialYou
            }
            .launchIn(viewModelScope)
    }

    /**
     * 更新深色模式
     */
    fun updateDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            sessionRepository.updateDarkTheme(isDark)
            // Flow 会自动更新 _darkTheme.value
        }
    }

    /**
     * 更新Material You
     */
    fun updateMaterialYou(useMaterialYou: Boolean) {
        viewModelScope.launch {
            sessionRepository.updateMaterialYou(useMaterialYou)
            // Flow 会自动更新 _materialYou.value
        }
    }

    /**
     * 切换一周的第一天
     */
    fun toggleFirstDayOfWeek() {
        val newValue = if (_firstDayOfWeek.value == 1) 7 else 1
        _firstDayOfWeek.value = newValue
        // 实际项目中需要保存到数据仓库
        viewModelScope.launch {
            // settingsRepository.updateFirstDayOfWeek(newValue)
        }
    }

    /**
     * 登出
     */
    fun logout() {
        viewModelScope.launch {
            try {
                clearLoginSessionUseCase()
                // 登出成功，导航操作由 UI 层 (AppNavigation) 处理
                Log.i("SettingsViewModel", "User logged out successfully.")
            } catch (e: Exception) {
                // 处理可能的错误，例如 DataStore 写入失败
                Log.e("SettingsViewModel", "Error during logout", e)
                // 可以在这里设置一个错误状态通知用户
            }
        }
    }

    /**
     * 清空数据库（仅开发使用）
     */
    fun clearDatabase() {
        viewModelScope.launch {
            _databaseOperation.value = DatabaseOperation.Loading
            try {
                devUtils.clearDatabaseAndDataStore()
                _databaseOperation.value = DatabaseOperation.Success("数据库已成功清空")
            } catch (e: Exception) {
                _databaseOperation.value = DatabaseOperation.Error("清空数据库失败: ${e.message}")
            }
        }
    }

    /**
     * 重置数据库操作状态
     */
    fun resetDatabaseOperation() {
        _databaseOperation.value = DatabaseOperation.Idle
    }
}

/**
 * 数据库操作状态
 */
sealed class DatabaseOperation {
    object Idle : DatabaseOperation()
    object Loading : DatabaseOperation()
    data class Success(val message: String) : DatabaseOperation()
    data class Error(val message: String) : DatabaseOperation()
} 