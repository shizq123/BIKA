package com.shizq.bika.core.network.plugin

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.encodedPath
import io.ktor.utils.io.KtorDsl
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import kotlin.time.Clock
import kotlin.uuid.Uuid


/**
 * 安装 Bika/Pica 漫画 API 签名与验证插件。
 */
public fun HttpClientConfig<*>.bikaAuth(block: BikaAuthConfig.() -> Unit) {
    install(BikaSignatureAuth) {
        block()
    }
}

/**
 * Bika 插件配置类
 */
@KtorDsl
public class BikaAuthConfig {
    public var apiKey: String = "C69BAF41DA5ABD1FFEDC6D2FEA56B"
    public var secretKey: String =
        $$"~d}$Q7$eIni=V)9\\RK/P.RM4;9[7|@/CA}b~OW!3?EV`:<>M7pddUBL5n|0/*Cn";
    public var appChannel: String = "2"
    public var appVersion: String = "2.2.1.3.3.4"
    public var appBuildVersion: String = "45"
    public var appPlatform: String = "android"
    public var appUuid: String = "defaultUuid"
    public var imageQuality: String = "original"
    public var userAgent: String = "okhttp/3.8.1"

    internal var tokenProvider: suspend () -> String? = { null }

    public fun token(block: suspend () -> String?) {
        tokenProvider = block
    }
}

public val BikaSignatureAuth: ClientPlugin<BikaAuthConfig> =
    createClientPlugin("BikaSignatureAuth", ::BikaAuthConfig) {

        val apiKey = pluginConfig.apiKey
        val secretKey = pluginConfig.secretKey
        val appChannel = pluginConfig.appChannel
        val appVersion = pluginConfig.appVersion
        val appBuildVersion = pluginConfig.appBuildVersion
        val appPlatform = pluginConfig.appPlatform
        val appUuid = pluginConfig.appUuid
        val imageQuality = pluginConfig.imageQuality
        val defaultUserAgent = pluginConfig.userAgent
        val tokenProvider = pluginConfig.tokenProvider

        on(Send) { request ->
            val nonce = Uuid.random().toString().replace("-", "")

            val time = (Clock.System.now().toEpochMilliseconds() / 1000).toString()

            val urlEnd = request.url.encodedPath.removePrefix("/")
            val type = request.method.value

            // urlEnd + time + nonce + type + apikey
            val rawData = urlEnd + time + nonce + type + apiKey
            val signature = HmacUtils(HmacAlgorithms.HMAC_SHA_256, secretKey).hmacHex(rawData)
            request.headers.apply {
                append("api-key", apiKey)
                append("accept", "application/vnd.picacomic.com.v1+json")
                append("app-channel", appChannel)
                append("time", time)
                append("nonce", nonce)
                append("signature", signature)
                append("app-version", appVersion)
                append("app-uuid", appUuid)
                append("image-quality", imageQuality)
                append("app-platform", appPlatform)
                append("app-build-version", appBuildVersion)

                if (!contains(HttpHeaders.UserAgent)) {
                    append(HttpHeaders.UserAgent, defaultUserAgent)
                }

                val token = tokenProvider()
                if (!token.isNullOrBlank()) {
                    if (request.url.encodedPath.contains("chat")) { // 简单判断示例
                        append(HttpHeaders.Authorization, "Bearer $token")
                        set(HttpHeaders.UserAgent, "Dart/2.19 (dart:io)")
                        set("api-version", "1.0.3")
                        set(HttpHeaders.ContentType, "application/json; charset=UTF-8")
                    } else {
                        append(HttpHeaders.Authorization, token)
                    }
                }
            }

            proceed(request)
        }
    }