package com.example.todoschedule.ui.home.model

sealed class HomeUiState {

    object Loading : HomeUiState()

    object Empty : HomeUiState()

    data class Error(val message: String) : HomeUiState()

    object NoTableSelected : HomeUiState()

    object Success : HomeUiState()
}