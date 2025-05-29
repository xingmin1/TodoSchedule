package com.example.todoschedule.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.model.User
import com.example.todoschedule.domain.repository.UserRepository
import com.example.todoschedule.domain.use_case.profile.GetUserProfileUseCase
import com.example.todoschedule.domain.use_case.profile.LogoutUseCase
import com.example.todoschedule.domain.use_case.profile.SaveAvatarUseCase
import com.example.todoschedule.domain.utils.Resource
import com.example.todoschedule.ui.profile.model.EditField
import com.example.todoschedule.ui.profile.model.ProfileEvent
import com.example.todoschedule.ui.profile.model.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val saveAvatarUseCase: SaveAvatarUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun handleEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.LoadProfile -> loadUserProfile()
            is ProfileEvent.StartEditField -> startEditing(event.field)
            is ProfileEvent.SaveEditedField -> saveField(event.field, event.value)
            is ProfileEvent.UpdateAvatar -> updateAvatar(event.uri)
            is ProfileEvent.CancelEdit -> cancelEdit()
            is ProfileEvent.Logout -> logout()
            is ProfileEvent.ClearMessages -> clearMessages()
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            getUserProfileUseCase().collect { result ->
                _uiState.update {
                    when (result) {
                        is Resource.Loading -> it.copy(
                            isLoading = true,
                            error = null,
                            successMessage = null
                        )

                        is Resource.Success -> it.copy(
                            user = result.data,
                            isLoading = false,
                            error = null
                        )

                        is Resource.Error -> it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    private fun startEditing(field: EditField) {
        _uiState.update { it.copy(editingField = field, error = null, successMessage = null) }
    }

    private fun cancelEdit() {
        _uiState.update { it.copy(editingField = null, error = null) }
    }

    private fun saveField(field: EditField, value: String) {
        if (field == EditField.AVATAR) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, successMessage = null) }
            val currentUser = uiState.value.user

            if (currentUser == null) {
                _uiState.update { it.copy(isSaving = false, error = "用户数据不存在，无法保存") }
                return@launch
            }

            try {
                val updatedUser: User = when (field) {
                    EditField.USERNAME -> {
                        if (value.isBlank() || value.length < 3) throw IllegalArgumentException("用户名长度不能少于3个字符")
                        currentUser.copy(username = value)
                    }

                    EditField.EMAIL -> {
                        if (value.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(value)
                                .matches()
                        ) throw IllegalArgumentException("邮箱格式不正确")
                        currentUser.copy(email = value.ifBlank { null })
                    }

                    EditField.PHONE -> {
                        if (value.isNotBlank() && !value.matches(Regex("^\\+?[0-9]{10,13}$"))) throw IllegalArgumentException(
                            "手机号格式不正确"
                        )
                        currentUser.copy(phoneNumber = value.ifBlank { null })
                    }

                    EditField.AVATAR -> currentUser
                }
                userRepository.updateUser(updatedUser)
                _uiState.update {
                    it.copy(
                        user = updatedUser,
                        isSaving = false,
                        editingField = null,
                        successMessage = "${
                            field.name.lowercase().replaceFirstChar { it.uppercase() }
                        } 更新成功"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "保存失败"
                    )
                }
            }
        }
    }

    private fun updateAvatar(uri: Uri) {
        saveAvatarUseCase(uri).onEach { result ->
            _uiState.update {
                when (result) {
                    is Resource.Loading -> it.copy(
                        isSaving = true,
                        error = null,
                        successMessage = null
                    )

                    is Resource.Success -> {
                        it.copy(
                            user = result.data,
                            isSaving = false,
                            successMessage = "头像更新成功"
                        )
                    }

                    is Resource.Error -> {
                        it.copy(
                            isSaving = false,
                            error = result.message ?: "头像更新失败"
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    private fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            val result = logoutUseCase()
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoggedOut = true, isLoading = false, user = null) }
            } else {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "退出登录失败",
                        isLoading = false
                    )
                }
            }
        }
    }
} 