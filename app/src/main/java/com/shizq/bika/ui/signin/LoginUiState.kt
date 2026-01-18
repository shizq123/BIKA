package com.shizq.bika.ui.signin

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = true,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
)

sealed interface LoginAction {
    data class AccountChanged(val account: String) : LoginAction
    data class PasswordChanged(val password: String) : LoginAction
    data class ToggleRememberMe(val checked: Boolean) : LoginAction

    // Action for when the process fails, is cancelled, or finds no credentials
    data class CredentialFetchFailed(val errorMessage: String?) : LoginAction

    data object LoginClicked : LoginAction
}