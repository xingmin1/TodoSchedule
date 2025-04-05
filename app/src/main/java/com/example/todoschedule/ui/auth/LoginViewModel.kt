package com.example.todoschedule.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.use_case.auth.LoginUserUseCase
import com.example.todoschedule.domain.use_case.auth.SaveLoginSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUserUseCase: LoginUserUseCase,
    private val saveLoginSessionUseCase: SaveLoginSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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

            val result = loginUserUseCase(username, password)

            result.onSuccess { user ->
                // 登录成功，保存用户会话
                saveLoginSessionUseCase(user.id.toLong()) // 假设 User ID 是 Int，需要转 Long
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