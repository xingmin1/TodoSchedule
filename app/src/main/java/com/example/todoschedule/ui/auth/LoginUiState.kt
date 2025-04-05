package com.example.todoschedule.ui.auth

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false // 标记登录是否成功，用于导航
) 