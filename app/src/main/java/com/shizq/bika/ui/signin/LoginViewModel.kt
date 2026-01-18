package com.shizq.bika.ui.signin

import android.content.Context
import android.util.Log
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.datastore.UserCredentialsDataSource
import com.shizq.bika.core.network.BikaDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userCredentialsDataSource: UserCredentialsDataSource,
    private val credentialManager: CredentialManager,
    private val savedStateHandle: SavedStateHandle,
    private val api: BikaDataSource,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    init {
        tryAutoSignIn()
    }

    fun onAccountChanged(account: String) {
        _uiState.update { it.copy(accountInput = account) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(passwordInput = password) }
    }

    fun onManualSignInClick() {
        val currentState = _uiState.value
        executeLogin(currentState.accountInput, currentState.passwordInput)
    }

    fun tryAutoSignIn() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAutoSignInAttempting = true) }

            val getCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(GetPasswordOption())
                .build()

            try {
                val result = credentialManager.getCredential(context, getCredentialRequest)
                handleCredentialResult(result)
            } catch (e: GetCredentialException) {
                Log.d("CredentialManager", "No credential available or user cancelled.", e)
            } finally {
                _uiState.update { it.copy(isAutoSignInAttempting = false) }
            }
        }
    }

    fun requestCredentialsForAutofill() {
        viewModelScope.launch {
            val getCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(GetPasswordOption())
                .build()

            try {
                val result = credentialManager.getCredential(context, getCredentialRequest)

                handleCredentialResultForAutofill(result)
            } catch (e: GetCredentialException) {

                Log.d("CredentialManager", "Autofill cancelled or no credential available.", e)
            }
        }
    }

    private fun handleCredentialResultForAutofill(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is PasswordCredential -> {

                val username = credential.id
                val password = credential.password
                _uiState.update {
                    it.copy(accountInput = username, passwordInput = password)
                }
            }

            else -> {
                Log.e(
                    "CredentialManager",
                    "Unexpected credential type for autofill: ${credential.type}"
                )
                _uiState.update { it.copy(errorMessage = "不支持的凭证类型") }
            }
        }
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun handleCredentialResult(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is PasswordCredential -> {
                val username = credential.id
                val password = credential.password
                executeLogin(username, password)
            }

            else -> {
                Log.e(
                    "CredentialManager",
                    "Unexpected credential type received: ${credential.type}"
                )
                _uiState.update { it.copy(errorMessage = "不支持的凭据类型") }
            }
        }
    }

    private fun executeLogin(account: String, pass: String) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val loginData = api.login(account, pass)
                userCredentialsDataSource.setToken(loginData.token)
                userCredentialsDataSource.setUsername(account)

                saveCredentials(account, pass)

                _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "登录失败: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun saveCredentials(username: String, pass: String) {
        try {
            val createPasswordRequest = CreatePasswordRequest(username, pass)

            credentialManager.createCredential(context, createPasswordRequest)
            Log.d("CredentialManager", "Credential save request sent successfully.")
        } catch (e: CreateCredentialException) {
            Log.e("CredentialManager", "Saving credentials failed or was cancelled by user.", e)
        }
    }
}