package com.shizq.bika.ui.signup

sealed interface RegistrationUiState {
    data object None : RegistrationUiState

    data object Loading : RegistrationUiState

    data object Success : RegistrationUiState

    data class Error(val message: String) : RegistrationUiState
}