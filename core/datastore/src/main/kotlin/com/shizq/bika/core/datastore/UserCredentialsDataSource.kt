package com.shizq.bika.core.datastore

import androidx.datastore.core.DataStore
import com.shizq.bika.core.datastore.model.UserCredentials
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val logger = KotlinLogging.logger("Credentials")

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
        token = token?.let { decryptWithLog(it, "token", cipher) },
        username = username?.let { decryptWithLog(it, "username", cipher) },
        password = password?.let { decryptWithLog(it, "password", cipher) },
    )

    /**
     * 解密凭证；历史明文（无 enc: 前缀）原样透传；
     * 密文解密失败（Keystore 密钥随备份恢复丢失、数据损坏）时记录告警，
     * 避免用户遇到"token 乱串导致登录失败"却无从排查。
     */
    private fun decryptWithLog(value: String, name: String, cipher: CredentialsCipher): String {
        val plain = cipher.decrypt(value)
        if (plain == null && cipher.isEncrypted(value)) {
            logger.error { "凭证 $name 解密失败（密钥丢失或数据损坏），将原样透传；建议重新登录" }
        }
        return plain ?: value
    }
}
