package com.example.todoschedule.ui.profile.model

import android.net.Uri

/**
 * Represents user events that can occur on the Profile screen.
 */
sealed class ProfileEvent {
    data object LoadProfile : ProfileEvent()
    data class StartEditField(val field: EditField) : ProfileEvent() // Renamed from EditField to avoid confusion with enum
    data class SaveEditedField(val field: EditField, val value: String) : ProfileEvent()
    data class UpdateAvatar(val uri: Uri) : ProfileEvent() // New event for avatar URI
    data object CancelEdit : ProfileEvent()
    data object Logout : ProfileEvent()
    data object ClearMessages : ProfileEvent() // To clear error or success messages from UIState
} 