package com.shizq.bika.core.network.plugin

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.withCharset
import io.ktor.utils.io.KtorDsl
import io.ktor.utils.io.charsets.Charsets
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import java.util.UUID

fun HttpClientConfig<*>.bikaAuth(block: BikaAuthConfig.() -> Unit = {}) {
    install(BikaSignatureAuth) {
        block()
    }
}

@KtorDsl
class BikaAuthConfig {
    var apiKey: String = "C69BAF41DA5ABD1FFEDC6D2FEA56B"
    var secretKey: String = "~d}\$Q7\$eIni=V)9\\RK/P.RM4;9[7|@/CA}b~OW!3?EV`:<>M7pddUBL5n|0/*Cn"

    var appChannel: String = "2"
    var appVersion: String = "2.2.1.2.3.3"
    var appBuildVersion: String = "44"
    var appUuid: String = "defaultUuid"
    var appPlatform: String = "android"

    internal var tokenProvider: suspend () -> String? = { null }
    fun token(block: suspend () -> String?) {
        tokenProvider = block
    }
}

/**
 * 插件主体
 */
val BikaSignatureAuth: ClientPlugin<BikaAuthConfig> =
    createClientPlugin("BikaSignatureAuth", ::BikaAuthConfig) {
        val config = pluginConfig

        onRequest { request, _ ->
            val nonce = UUID.randomUUID().toString().replace("-", "")
            val time = (System.currentTimeMillis() / 1000).toString()

            val urlPath = request.url.encodedPath
            val method = request.method.value

            val rawData = (urlPath + time + nonce + method + config.apiKey).lowercase()

            val signature =
                HmacUtils(HmacAlgorithms.HMAC_SHA_256, config.secretKey).hmacHex(rawData)

            request.contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
            request.headers.apply {

                append("api-key", config.apiKey)
                append("accept", "application/vnd.picacomic.com.v1+json")
                append("app-channel", config.appChannel)
                append("time", time)
                append("nonce", nonce)
                append("signature", signature)

                append("app-version", config.appVersion)
                append("app-uuid", config.appUuid)
                append("app-platform", config.appPlatform)
                append("app-build-version", config.appBuildVersion)

                append("image-quality", "original")
                append("User-Agent", "okhttp/3.8.1")

                config.tokenProvider()?.let {
                    append(HttpHeaders.Authorization, it)
                }
            }
        }

        on(Send) { request ->
            request.headers.run {
                remove(HttpHeaders.AcceptCharset)
                remove(HttpHeaders.Accept, "application/json")
            }
            proceed(request)
        }
    }