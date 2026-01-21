package com.shizq.bika.ui.signin

import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.network.BikaDataSource
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first

class LoginStateMachine @Inject constructor(
    private val passwordCredentialManager: PasswordCredentialManager,
    private val userCredentialsDataSource: UserCredentialsDataSource,
    private val api: BikaDataSource,
) : FlowReduxStateMachineFactory<LoginUiState, LoginAction>() {
    init {
        initializeWith { LoginUiState() }
        spec {
            inState<LoginUiState> {
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
                        userCredentialsDataSource.setToken(loginData.token)
                        userCredentialsDataSource.setUsername(snapshot.username)
                        if (snapshot.rememberMe) {
                            passwordCredentialManager.savePasswordCredential(
                                snapshot.username,
                                snapshot.password
                            )
                        }
                        mutate { copy(isLoading = false, isSuccess = true) }
                    } catch (e: Exception) {
                        mutate { copy(isLoading = false, errorMessage = e.message ?: "登录失败") }
                    }
                }
                condition({ it.isLoading }) {

                }
                collectWhileInState(passwordCredentialManager.getPasswordCredential()) { result ->
                    when (result) {
                        is CredentialResult.Success -> mutate {
                            copy(
                                username = result.username,
                                password = result.password
                            )
                        }

                        is CredentialResult.Error -> fillFromUserData(result.message)
                        CredentialResult.NoCredentialFound -> fillFromUserData(null)
                        CredentialResult.Cancelled -> fillFromUserData(null)
                    }
                }
            }
        }
    }

    private suspend fun ChangeableState<LoginUiState>.fillFromUserData(errorMessage: String?): ChangedState<LoginUiState> {
        val data = userCredentialsDataSource.userData.first()
        return mutate {
            copy(
                errorMessage = errorMessage,
                username = data.username ?: "",
                password = data.password ?: ""
            )
        }
    }
}