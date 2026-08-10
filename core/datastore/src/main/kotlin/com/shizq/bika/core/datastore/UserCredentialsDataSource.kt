package com.shizq.bika.core.datastore

import androidx.datastore.core.DataStore
import com.shizq.bika.core.datastore.model.UserCredentials
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserCredentialsDataSource @Inject constructor(
    private val userCredentials: DataStore<UserCredentials>,
    private val credentialsCipher: CredentialsCipher,
) {
    /**
     * 对外暴露解密后的明文凭证；历史明文数据自动透传（解密返回 null 时原样使用）。
     */
    val userData: Flow<UserCredentials> = userCredentials.data.map { it.decrypt(credentialsCipher) }

    suspend fun setToken(token: String?) {
        userCredentials.updateData {
            it.copy(token = token?.encrypt(credentialsCipher))
        }
    }

    suspend fun setUsername(username: String?) {
        userCredentials.updateData {
            it.copy(username = username?.encrypt(credentialsCipher))
        }
    }

    suspend fun setPassword(password: String?) {
        userCredentials.updateData {
            it.copy(password = password?.encrypt(credentialsCipher))
        }
    }

    private fun String.encrypt(cipher: CredentialsCipher): String = cipher.encrypt(this)

    private fun UserCredentials.decrypt(cipher: CredentialsCipher): UserCredentials = copy(
        token = token?.let { cipher.decrypt(it) ?: it },
        username = username?.let { cipher.decrypt(it) ?: it },
        password = password?.let { cipher.decrypt(it) ?: it },
    )
}
