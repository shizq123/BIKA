package com.shizq.bika.ui.signin

import androidx.compose.runtime.Immutable

@Immutable
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val rememberPassword: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isAuthenticating: Boolean = false,
    val usernameIsEmpty: Boolean = false,
    val passwordIsEmpty: Boolean = false
)

sealed interface LoginAction {
    data class AccountChanged(val account: String) : LoginAction
    data class PasswordChanged(val password: String) : LoginAction
    data class ToggleRememberPassword(val checked: Boolean) : LoginAction

    data object ClearError : LoginAction
    data object SubmitLogin : LoginAction
    data object ResetSuccessState : LoginAction
}