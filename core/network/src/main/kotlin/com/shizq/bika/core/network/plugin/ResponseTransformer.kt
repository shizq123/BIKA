package com.shizq.bika.core.network.plugin

import com.shizq.bika.core.network.model.Box
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey
import io.ktor.utils.io.KtorDsl
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer

val ExpectRawResponse = AttributeKey<Unit>("ExpectRawResponse")

@KtorDsl
class ApiEnvelopeConfig {
    /**
     * 响应体 `code=401` 时的回调（通路 B）。
     *
     * Bika 大量接口以 HTTP 200 + 信封内 `code=401` 表达鉴权失败，
     * 这类响应在传输层看不出异常，只能在解包时识别。默认空实现，
     * 由 NetworkModule 接到 SessionManager。
     */
    internal var onUnauthorized: suspend () -> Unit = {}

    fun onUnauthorized(block: suspend () -> Unit) {
        onUnauthorized = block
    }
}

val ApiEnvelopePlugin: ClientPlugin<ApiEnvelopeConfig> =
    createClientPlugin("ResponseTransformer", ::ApiEnvelopeConfig) {
        val json = Json { ignoreUnknownKeys = true }
        val onUnauthorized = pluginConfig.onUnauthorized

        transformResponseBody { response, content, requestedType ->
            if (response.request.attributes.getOrNull(ExpectRawResponse) != null) {
                return@transformResponseBody null
            }
            if (!response.status.isSuccess() || requestedType.type == Unit::class) {
                return@transformResponseBody content
            }

            val targetKotlinType = requestedType.kotlinType ?: return@transformResponseBody content

            val decodedContent = json.decodeFromStream(
                Box.serializer(serializer(targetKotlinType)),
                content.toInputStream()
            )
            if (decodedContent.code == HttpStatusCode.Unauthorized.value) {
                onUnauthorized()
                throw UnauthorizedException(decodedContent.message)
            }
            if (decodedContent.code != HttpStatusCode.OK.value) {
                throw ApiException(decodedContent.code, decodedContent.message)
            }
            decodedContent.data
        }
    }

class ApiException(val code: Int, message: String) : Exception("API Error ($code): $message")
class UnauthorizedException(message: String) : Exception(message)
