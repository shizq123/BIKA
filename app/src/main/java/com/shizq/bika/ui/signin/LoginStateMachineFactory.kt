package com.shizq.bika.ui.signin

import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.network.BikaDataSource
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first

class LoginStateMachineFactory @Inject constructor(
    private val userCredentialsDataSource: UserCredentialsDataSource,
    private val api: BikaDataSource
) : FlowReduxStateMachineFactory<LoginUiState, LoginAction>() {

    init {
        initializeWith { LoginUiState() }

        spec {
            inState<LoginUiState> {
                onEnter { loadInitialCredentials() }

                on<LoginAction.AccountChanged> {
                    mutate {
                        copy(
                            username = it.account,
                            errorMessage = null
                        )
                    }
                }

                on<LoginAction.PasswordChanged> {
                    mutate {
                        copy(
                            password = it.password,
                            errorMessage = null
                        )
                    }
                }

                on<LoginAction.ToggleRememberPassword> {
                    mutate { copy(rememberPassword = it.checked) }
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
                on<LoginAction.ResetSuccessState> {
                    mutate { copy(isSuccess = false) }
                }
                onActionStartStateMachine<LoginAction.SubmitLogin, AuthState>(
                    stateMachineFactoryBuilder = {
                        AuthenticationStateMachineFactory(
                            api = api,
                            userCredentialsDataSource = userCredentialsDataSource,
                            username = snapshot.username,
                            password = snapshot.password,
                            rememberPassword = snapshot.rememberPassword
                        )

                    }
                ) { childState ->
                    when (childState) {
                        is AuthState.Authenticating -> mutate {
                            copy(
                                isAuthenticating = true,
                                errorMessage = null
                            )
                        }

                        is AuthState.Success -> mutate {
                            copy(
                                isAuthenticating = false,
                                isSuccess = true
                            )
                        }

                        is AuthState.Error -> mutate {
                            copy(
                                isAuthenticating = false,
                                errorMessage = childState.message
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun ChangeableState<LoginUiState>.loadInitialCredentials(): ChangedState<LoginUiState> {
        val credentials = userCredentialsDataSource.userData.first()

        return mutate {
            val newUsername = username.ifEmpty { credentials.username ?: "" }
            val newPassword = password.ifEmpty { credentials.password ?: "" }

            copy(
                username = newUsername,
                password = newPassword,
                rememberPassword = !credentials.password.isNullOrEmpty()
            )
        }
    }
}