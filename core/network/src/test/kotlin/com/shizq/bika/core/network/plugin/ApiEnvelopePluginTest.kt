package com.shizq.bika.core.network.plugin

import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.auth.SkipSessionExpiry
import com.shizq.bika.core.network.model.LoginResult
import com.shizq.bika.core.network.model.LoginTokenPayload
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.test.runTest
import kotlinx.io.readString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ApiEnvelopePluginTest {
    @Test
    fun `HTTP login errors preserve the server message without expiring the session`() = runTest {
        for (status in listOf(HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized)) {
            var expiryCount = 0
            client(status, """{"code":${status.value},"message":"用户名或密码错误"}""") {
                expiryCount++
            }.use { client ->
                assertEquals(
                    LoginResult.Rejected("用户名或密码错误"),
                    BikaDataSource(client).login("invalid", "wrong"),
                )
                assertEquals(0, expiryCount)
            }
        }
    }

    @Test
    fun `HTTP errors accept a message without a complete envelope`() = runTest {
        client(HttpStatusCode.BadRequest, """{"message":"邮箱格式不正确"}""").use { client ->
            val error = assertFailsWith<ApiException> {
                client.get("test").body<LoginTokenPayload>()
            }
            assertEquals(400, error.code)
            assertEquals("邮箱格式不正确", error.serverMessage)
        }
    }

    @Test
    fun `invalid error bodies produce a readable HTTP fallback`() = runTest {
        val bodies = listOf(
            "", "<html>Bad gateway</html>", "{", "[]", "null",
            """{"message":" "}""", """{"message":null}""", """{"message":42}""",
            """{"message":{"detail":"bad"}}""",
        )
        for (body in bodies) {
            client(HttpStatusCode.BadGateway, body).use { client ->
                val error = assertFailsWith<ApiException> {
                    client.get("test").body<LoginTokenPayload>()
                }
                assertEquals(502, error.code)
                assertEquals("请求失败（HTTP 502）", error.serverMessage)
            }
        }
    }

    @Test
    fun `successful login still unwraps the token`() = runTest {
        client(HttpStatusCode.OK, """{"code":200,"message":"OK","data":{"token":"token"}}""")
            .use { client ->
                assertEquals(LoginResult.Success("token"), BikaDataSource(client).login("a@b.c", "pass"))
            }
    }

    @Test
    fun `business unauthorized errors respect the login exemption`() = runTest {
        var expiryCount = 0
        client(HttpStatusCode.OK, """{"code":401,"message":"登录失败"}""") {
            expiryCount++
        }.use { client ->
            assertIs<LoginResult.Rejected>(BikaDataSource(client).login("a@b.c", "wrong"))
            assertEquals(0, expiryCount)
            assertFailsWith<UnauthorizedException> { client.get("test").body<LoginTokenPayload>() }
            assertEquals(1, expiryCount)
        }
    }

    @Test
    fun `raw responses bypass envelope handling even on HTTP errors`() = runTest {
        client(HttpStatusCode.BadRequest, """{"token":"raw"}""").use { client ->
            val payload = client.get("test") {
                attributes.put(ExpectRawResponse, Unit)
                attributes.put(SkipSessionExpiry, Unit)
            }.body<LoginTokenPayload>()
            assertEquals("raw", payload.token)
        }
    }

    @Test
    fun `missing Kotlin type leaves the body available to the next transformer`() = runTest {
        val fallback = createClientPlugin("Fallback") {
            transformResponseBody { _, content, requestedType ->
                if (requestedType.type == LoginTokenPayload::class) {
                    LoginTokenPayload(content.readRemaining().readString())
                } else null
            }
        }
        HttpClient(MockEngine { respond("untouched") }) {
            install(ApiEnvelopePlugin)
            install(fallback)
        }.use { client ->
            val payload = client.get("https://example.test")
                .body<LoginTokenPayload>(TypeInfo(LoginTokenPayload::class))
            assertEquals("untouched", assertIs<LoginTokenPayload>(payload).token)
        }
    }

    private fun client(
        status: HttpStatusCode,
        body: String,
        onUnauthorized: suspend () -> Unit = {},
    ) = HttpClient(MockEngine {
        respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
    }) {
        expectSuccess = false
        defaultRequest {
            url("https://example.test/")
            contentType(ContentType.Application.Json)
        }
        install(ApiEnvelopePlugin) { onUnauthorized(onUnauthorized) }
        install(ContentNegotiation) { json() }
    }
}
