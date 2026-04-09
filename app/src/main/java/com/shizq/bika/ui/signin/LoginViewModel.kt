package com.shizq.bika.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    loginStateMachine: LoginStateMachine,
    private val userCredentialsDataSource: UserCredentialsDataSource,
    private val api: BikaDataSource
) : ViewModel() {
    private val stateMache = loginStateMachine.launchIn(viewModelScope)
    val state = stateMache.state

    fun dispatch(action: LoginAction) {
        viewModelScope.launch {
            stateMache.dispatch(action)
        }
    }

    fun onLoginClick(username: String, password: String, rememberPassword: Boolean) {
        viewModelScope.launch {
            stateMache.dispatch(LoginAction.LoginStart)
            when (val result = api.login(username, password)) {
                is Result.Loading -> {}
                is Result.Success -> {
                    userCredentialsDataSource.setToken(result.data.token)
                    userCredentialsDataSource.setUsername(username)
                    userCredentialsDataSource.setPassword(if (rememberPassword) password else "")
                    stateMache.dispatch(LoginAction.LoginSuccess)
                }
                is Result.Error -> {
                    stateMache.dispatch(LoginAction.LoginFailed(result.exception.message))
                }
            }
        }
    }
}