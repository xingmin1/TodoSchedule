package com.example.todoschedule.ui.profile.model

import com.example.todoschedule.domain.model.User

/**
 * Represents the UI state for the Profile screen.
 */
data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val editingField: EditField? = null,
    val isLoggedOut: Boolean = false
)

/**
 * Enum representing the fields that can be edited on the Profile screen.
 */
enum class EditField {
    USERNAME,
    EMAIL,
    PHONE,
    AVATAR
} 