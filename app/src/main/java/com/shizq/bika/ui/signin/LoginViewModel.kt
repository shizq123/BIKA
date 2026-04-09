package com.shizq.bika.ui.signin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.network.BikaDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class LoginViewModel @Inject constructor(
    loginStateMachine: LoginStateMachine,
    private val userCredentialsDataSource: UserCredentialsDataSource,
    private val api: BikaDataSource
) : ViewModel() {
    private val stateMachine = loginStateMachine.launchIn(viewModelScope)
    val state = stateMachine.state

    fun dispatch(action: LoginAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }

    fun onLoginClick(username: String, password: String, rememberPassword: Boolean) {
        viewModelScope.launch {
            stateMachine.dispatch(LoginAction.LoginStart)
            try {
                val loginResult = api.login(username, password)

                if (!loginResult.token.isNullOrEmpty()) {
                    Log.d(TAG, "登录成功: Token获取正常")

                    userCredentialsDataSource.setToken(loginResult.token)
                    userCredentialsDataSource.setUsername(username)
                    userCredentialsDataSource.setPassword(if (rememberPassword) password else null)

                    stateMachine.dispatch(LoginAction.LoginSuccess)
                } else {
                    Log.w(TAG, "登录请求完成，但业务验证失败: ${loginResult.message}")

                    val errorMsg = when (loginResult.message) {
                        "invalid email or password" -> "用户名或密码错误"
                        else -> loginResult.message ?: "未知错误，请重试"
                    }
                    stateMachine.dispatch(LoginAction.LoginFailed(errorMsg))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "登录请求异常", e)
                stateMachine.dispatch(LoginAction.LoginFailed(e.message ?: "网络请求失败"))
            }
        }
    }
}

private const val TAG = "LoginViewModel"