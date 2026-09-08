package com.shizq.bika.core.network.plugin

import com.shizq.bika.core.network.auth.SkipSessionExpiry
import com.shizq.bika.core.network.model.ApiEnvelope
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey
import io.ktor.utils.io.KtorDsl
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
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

/**
 * 解包 Bika 接口统一的 `{code, message, data}` 信封的插件。
 *
 * 命名统一为 "ApiEnvelope"：文件名、注册名与业务含义保持一致，避免
 * 日志里出现的注册名与源码文件名不对应导致排查时找不到人。
 */
val ApiEnvelopePlugin: ClientPlugin<ApiEnvelopeConfig> =
    createClientPlugin("ApiEnvelope", ::ApiEnvelopeConfig) {
        val json = Json { ignoreUnknownKeys = true }
        val onUnauthorized = pluginConfig.onUnauthorized

        transformResponseBody { response, content, requestedType ->
            if (response.request.attributes.getOrNull(ExpectRawResponse) != null) {
                return@transformResponseBody null
            }
            if (!response.status.isSuccess()) {
                return@transformResponseBody content
            }

            // 登录接口等匿名/表单类请求的 401 是"密码错了"而非"会话过期"：
            // 跳过 onUnauthorized 回调（不终止会话、不弹全局提示），但仍抛出，
            // 由调用方（如 BikaDataSource.login）捕获后转成表单内的业务错误展示。
            val skipSessionExpiry = response.request.attributes.getOrNull(SkipSessionExpiry) != null

            // Unit 请求（写操作只关心成功/失败，不关心 data）也要走信封校验，
            // 否则 code!=200 的业务失败会被这里直接放行，调用方误以为操作成功。
            if (requestedType.type == Unit::class) {
                val envelope = json.decodeFromStream(
                    ApiEnvelope.serializer(JsonElement.serializer()),
                    content.toInputStream()
                )
                if (envelope.code == HttpStatusCode.Unauthorized.value) {
                    if (!skipSessionExpiry) onUnauthorized()
                    throw UnauthorizedException(envelope.message)
                }
                if (envelope.code != HttpStatusCode.OK.value) {
                    throw ApiException(envelope.code, envelope.message)
                }
                return@transformResponseBody Unit
            }

            val targetKotlinType = requestedType.kotlinType ?: return@transformResponseBody content

            val decodedContent = json.decodeFromStream(
                ApiEnvelope.serializer(serializer(targetKotlinType)),
                content.toInputStream()
            )
            if (decodedContent.code == HttpStatusCode.Unauthorized.value) {
                if (!skipSessionExpiry) onUnauthorized()
                throw UnauthorizedException(decodedContent.message)
            }
            if (decodedContent.code != HttpStatusCode.OK.value) {
                throw ApiException(decodedContent.code, decodedContent.message)
            }
            decodedContent.data
        }
    }
