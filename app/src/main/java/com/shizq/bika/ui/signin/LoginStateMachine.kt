package com.shizq.bika.ui.signin

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first

class LoginStateMachine @Inject constructor(
    private val userCredentialsDataSource: UserCredentialsDataSource
) : FlowReduxStateMachineFactory<LoginUiState, LoginAction>() {
    init {
        initializeWith { LoginUiState() }
        spec {
            inState<LoginUiState> {
                onEnter {
                    val credentials = userCredentialsDataSource.userData.first()
                    mutate {
                        copy(
                            isLoading = false,
                            username = credentials.username ?: "",
                            password = credentials.password ?: ""
                        )
                    }
                }
                on<LoginAction.AccountChanged> {
                    mutate {
                        copy(
                            isLoading = false,
                            username = it.account,
                            usernameIsEmpty = !it.account.isNotEmpty(),
                            errorMessage = null
                        )
                    }
                }
                on<LoginAction.PasswordChanged> {
                    mutate {
                        copy(
                            isLoading = false,
                            password = it.password,
                            passwordIsEmpty = !it.password.isNotEmpty(),
                            errorMessage = null
                        )
                    }
                }
                on<LoginAction.ToggleRememberPassword> {
                    mutate { copy(rememberPassword = it.checked) }
                }
                on<LoginAction.LoginStart> {
                    mutate { copy(isLoading = true) }
                }
                on<LoginAction.LoginSuccess> {
                    mutate { copy(isLoading = false, isSuccess = true) }
                }
                on<LoginAction.LoginFailed> {
                    mutate {
                        copy(
                            isLoading = false,
                            errorMessage = it.msg ?: "登录失败"
                        )
                    }
                }
                on<LoginAction.ClearError> {
                    val rememberPassword = snapshot.rememberPassword
                    val password = snapshot.password
                    mutate {
                        copy(
                            isSuccess = false,
                            password = if (rememberPassword) password else "",
                            errorMessage = null
                        )
                    }
                }
            }
        }
    }
}