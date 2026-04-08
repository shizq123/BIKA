package com.shizq.bika.ui.signin

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val rememberPassword: Boolean = true,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val usernameIsEmpty: Boolean = false,
    val passwordIsEmpty: Boolean = false
)

sealed interface LoginAction {
    data class AccountChanged(val account: String) : LoginAction
    data class PasswordChanged(val password: String) : LoginAction
    data class ToggleRememberPassword(val checked: Boolean) : LoginAction

    data object ClearError : LoginAction
    data object LoginClicked : LoginAction

    data object LoginStart : LoginAction
    data object LoginSuccess : LoginAction
    data class LoginFailed(val msg: String?) : LoginAction
}