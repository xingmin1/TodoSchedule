package com.example.todoschedule.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.core.utils.DevUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页面ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application
) : AndroidViewModel(application) {

    // 深色模式
    private val _darkTheme = MutableStateFlow(false)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    // 是否使用Material You
    private val _materialYou = MutableStateFlow(true)
    val materialYou: StateFlow<Boolean> = _materialYou.asStateFlow()

    // 一周的第一天（1:周一，7:周日）
    private val _firstDayOfWeek = MutableStateFlow(1)
    val firstDayOfWeek: StateFlow<Int> = _firstDayOfWeek.asStateFlow()

    // 数据库操作状态
    private val _databaseOperation = MutableStateFlow<DatabaseOperation>(DatabaseOperation.Idle)
    val databaseOperation: StateFlow<DatabaseOperation> = _databaseOperation.asStateFlow()

    /**
     * 更新深色模式
     */
    fun updateDarkTheme(darkTheme: Boolean) {
        _darkTheme.value = darkTheme
        // 实际项目中需要保存到数据仓库
        viewModelScope.launch {
            // settingsRepository.updateDarkTheme(darkTheme)
        }
    }

    /**
     * 更新Material You
     */
    fun updateMaterialYou(materialYou: Boolean) {
        _materialYou.value = materialYou
        // 实际项目中需要保存到数据仓库
        viewModelScope.launch {
            // settingsRepository.updateMaterialYou(materialYou)
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
     * 清空数据库（仅开发使用）
     */
    fun clearDatabase() {
        viewModelScope.launch {
            _databaseOperation.value = DatabaseOperation.Loading
            try {
                DevUtils.clearDatabase(application)
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