package com.shizq.bika.ui.signin

import com.freeletics.flowredux2.ChangeableState
import com.freeletics.flowredux2.ChangedState
import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.network.BikaDataSource
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

class AuthenticationStateMachineFactory(
    private val api: BikaDataSource,
    private val userCredentialsDataSource: UserCredentialsDataSource,
    private val username: String,
    private val password: String,
    private val rememberPassword: Boolean
) : FlowReduxStateMachineFactory<AuthState, LoginAction>() {
    init {
        initializeWith { AuthState.Authenticating }
        spec {
            inState<AuthState.Authenticating> {
                onEnter { performLogin() }
            }
        }
    }

    private suspend fun ChangeableState<AuthState.Authenticating>.performLogin(): ChangedState<AuthState> {
        return try {
            if (username.isBlank() || password.isEmpty()) {
                return override { AuthState.Error("账号和密码不能为空") }
            }

            val loginResult = api.login(username, password)

            if (!loginResult.token.isNullOrEmpty()) {
                userCredentialsDataSource.setToken(loginResult.token)
                userCredentialsDataSource.setUsername(username)
                userCredentialsDataSource.setPassword(if (rememberPassword) password else null)
                override { AuthState.Success }
            } else {
                val errorMsg = when (loginResult.message) {
                    "invalid email or password" -> "用户名或密码错误"
                    else -> loginResult.message ?: "未知错误，请重试"
                }
                override { AuthState.Error(errorMsg) }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val userFriendlyMessage = when (e) {
                is UnknownHostException -> "无网络连接，请检查网络设置"
                is SocketTimeoutException -> "网络请求超时，请稍后重试"
                is IOException -> "网络连接异常"
                else -> "未知系统错误"
            }
            override { AuthState.Error(userFriendlyMessage) }
        }
    }
}

sealed interface AuthState {
    data object Authenticating : AuthState
    data object Success : AuthState
    data class Error(val message: String) : AuthState
}