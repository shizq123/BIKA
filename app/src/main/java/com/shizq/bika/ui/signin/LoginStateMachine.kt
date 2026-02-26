package com.shizq.bika.ui.signin

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.network.BikaDataSource
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first

class LoginStateMachine @Inject constructor(
    private val userCredentialsDataSource: UserCredentialsDataSource,
    private val api: BikaDataSource,
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
                    mutate { copy(username = it.account) }
                }
                on<LoginAction.PasswordChanged> {
                    mutate { copy(password = it.password) }
                }
                on<LoginAction.ToggleRememberMe> {
                    mutate { copy(rememberMe = it.checked) }
                }
                on<LoginAction.LoginClicked> {
                    if (snapshot.username.isBlank()) {
                        return@on mutate { copy(errorMessage = "请输入账号") }
                    }

                    if (snapshot.password.isBlank()) {
                        return@on mutate { copy(errorMessage = "请输入密码") }
                    }

                    try {
                        val loginData = api.login(snapshot.username, snapshot.password)
                        if (snapshot.rememberMe) {
                            userCredentialsDataSource.setUsername(snapshot.username)
                            userCredentialsDataSource.setPassword(snapshot.password)
                            userCredentialsDataSource.setToken(loginData.token)
                        }
                        mutate { copy(isLoading = false, isSuccess = true) }
                    } catch (e: Exception) {
                        mutate { copy(isLoading = false, errorMessage = e.message ?: "登录失败") }
                    }
                }
                condition({ it.isLoading }) {

                }
            }
        }
    }
}