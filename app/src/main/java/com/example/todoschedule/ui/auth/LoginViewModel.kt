package com.example.todoschedule.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.use_case.auth.LoginUserUseCase
import com.example.todoschedule.domain.use_case.auth.SaveLoginSessionUseCase
import com.example.todoschedule.data.sync.SyncManager
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUserUseCase: LoginUserUseCase,
    private val saveLoginSessionUseCase: SaveLoginSessionUseCase,
    private val syncManager: SyncManager
) : ViewModel() {

    private val TAG = "LoginViewModel"
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // 创建一个应用级别的协程作用域，不会随ViewModel的销毁而取消
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val username = _uiState.value.username
            val password = _uiState.value.password

            val result = loginUserUseCase(username, password, useRemote = true)

            result.onSuccess { user ->
                // 登录成功，保存用户会话
                saveLoginSessionUseCase(user.id.toLong(), user.token) // 保存用户ID和token

                // 触发立即同步，使用应用级别的协程作用域
                applicationScope.launch {
                    try {
                        // 确保令牌已保存
                        withContext(Dispatchers.Main) {
                            Log.d(TAG, "登录成功，立即触发同步操作")
                        }

                        // 短暂延迟，确保令牌已经被保存
                        delay(1000)

                        // 先尝试注册设备
                        val userId = user.id
                        Log.d(TAG, "尝试注册设备，用户ID: $userId")

                        val syncRepo = syncManager.getSyncRepository()
                        val deviceRegistered = syncRepo.registerDevice(userId)

                        if (deviceRegistered) {
                            Log.d(TAG, "设备注册成功，开始同步")
                            // 主动触发同步
                            val syncResult = syncManager.syncNow()

                            withContext(Dispatchers.Main) {
                                if (syncResult) {
                                    Log.d(TAG, "登录后同步成功")
                                } else {
                                    Log.d(TAG, "登录后同步未完全成功")
                                }
                            }
                        } else {
                            Log.e(TAG, "设备注册失败，跳过同步")
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e(TAG, "登录后同步失败", e)
                        }
                    }
                }
                
                _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
            }.onFailure { exception ->
                // 登录失败，显示错误信息
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "登录失败"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
} 