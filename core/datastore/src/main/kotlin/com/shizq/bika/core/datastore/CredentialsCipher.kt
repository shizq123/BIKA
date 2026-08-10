package com.shizq.bika.core.datastore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import jakarta.inject.Inject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 使用 Android Keystore 中不可导出的 AES-256-GCM 密钥对用户凭证（token/password）加解密。
 *
 * 密文格式：`enc:<base64(iv + ciphertext + gcm tag)>`。
 * 无 `enc:` 前缀的值视为历史明文数据，[decrypt] 返回 null，由调用方原样透传，
 * 实现旧数据无缝兼容（下次写入时自动升级为密文）。
 */
class CredentialsCipher @Inject constructor() {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    private val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(plain: String): String {
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    /**
     * @return 明文；若密文无效或值为历史明文（无前缀），返回 null
     */
    fun decrypt(value: String): String? {
        if (!value.startsWith(PREFIX)) return null
        return try {
            val raw = Base64.decode(value.removePrefix(PREFIX), Base64.NO_WRAP)
            val iv = raw.copyOfRange(0, IV_LENGTH)
            val encrypted = raw.copyOfRange(IV_LENGTH, raw.size)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "bika_user_credentials_key"
        const val PREFIX = "enc:"
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
    }
}
