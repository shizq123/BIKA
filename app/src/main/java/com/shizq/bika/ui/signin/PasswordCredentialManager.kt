package com.shizq.bika.ui.signin

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.shizq.bika.core.coroutine.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class PasswordCredentialManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val credentialManager = CredentialManager.create(context)

    init {
        scope.launch {
            prepareGetCredential()
        }
    }

    suspend fun prepareGetCredential() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val getPasswordOption = GetPasswordOption()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(getPasswordOption)
                .build()
            credentialManager.prepareGetCredential(request)
        }
    }

    suspend fun savePasswordCredential(username: String, password: String) {
        val request = CreatePasswordRequest(id = username, password = password)
        try {
            credentialManager.createCredential(context, request)
        } catch (e: Exception) {
            Log.e(TAG, "Save credential failed", e)
        }
    }

    /**
     * 触发系统UI来获取用户选择的密码凭据。
     */
    fun getPasswordCredential(): Flow<CredentialResult> = flow {
        val getPasswordOption = GetPasswordOption()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(getPasswordOption)
            .build()

        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is PasswordCredential) {
                emit(CredentialResult.Success(credential.id, credential.password))
            } else {
                emit(CredentialResult.Error("Returned credential is not a PasswordCredential"))
            }
        } catch (e: NoCredentialException) {
            Log.i(TAG, "No credential found.", e)
            emit(CredentialResult.NoCredentialFound)
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User cancelled credential selection.", e)
            emit(CredentialResult.Cancelled)
        } catch (e: Exception) {
            Log.e(TAG, "Get credential failed with an exception.", e)
            emit(CredentialResult.Error("An unexpected error occurred", e))
        }
    }.flowOn(Dispatchers.IO)
}

sealed class CredentialResult {
    data class Success(val username: String, val password: String) : CredentialResult()
    object Cancelled : CredentialResult()
    object NoCredentialFound : CredentialResult()
    data class Error(val message: String, val throwable: Throwable? = null) : CredentialResult()
}

private const val TAG = "CredentialHelper"