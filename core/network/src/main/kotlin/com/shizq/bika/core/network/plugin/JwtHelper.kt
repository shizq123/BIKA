package com.shizq.bika.core.network.plugin

import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

@Serializable
data class BikaUserPayload(
    @SerialName("buildVersion")
    val buildVersion: String = "",
    @SerialName("email")
    val email: String = "",
    @SerialName("exp")
    val exp: Int = 0,
    @SerialName("iat")
    val iat: Int = 0,
    @SerialName("_id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("platform")
    val platform: String = "",
    @SerialName("role")
    val role: String = "",
    @SerialName("version")
    val version: String = ""
)

object JwtHelper {
    private val json = Json { ignoreUnknownKeys = true }

    fun decodePayload(token: String): BikaUserPayload? {
        try {
            val parts = token.split(".")
            if (parts.size < 2) return null

            val payloadJson = Base64.UrlSafe.decode(parts[1]).decodeToString()

            return json.decodeFromString<BikaUserPayload>(payloadJson)
        } catch (e: Exception) {
            Log.d("JwtHelper", "DecodePayload error", e)
            return null
        }
    }
}