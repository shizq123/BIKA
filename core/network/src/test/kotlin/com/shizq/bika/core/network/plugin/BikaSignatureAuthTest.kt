package com.shizq.bika.core.network.plugin

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BikaSignatureAuthTest {

    @Test
    fun `标准请求生成正确的相对路径签名与头部`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(lastRequest)

        client.get("https://picaapi.picacomic.com/auth/sign-in")

        val request = assertNotNull(lastRequest.get())
        assertHeadersAndSignature(
            request = request,
            expectedPath = "auth/sign-in",
            expectedMethod = "GET",
        )
    }

    @Test
    fun `带 Query 参数请求签名完整保留相对路径与查询字符串`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(lastRequest)

        client.get("https://picaapi.picacomic.com/comics") {
            parameter("page", 2)
            parameter("c", "tag")
        }

        val request = assertNotNull(lastRequest.get())
        // 验证签名计算保留完整相对路径与 Query 字符串，对齐服务端校验契约
        assertHeadersAndSignature(
            request = request,
            expectedPath = "comics?page=2&c=tag",
            expectedMethod = "GET",
        )
    }

    @Test
    fun `自定义域名与端口下签名与标准域名保持一致`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(lastRequest)

        client.get("https://custom-mirror.example.com:8443/comics/12345/eps")

        val request = assertNotNull(lastRequest.get())
        assertHeadersAndSignature(
            request = request,
            expectedPath = "comics/12345/eps",
            expectedMethod = "GET",
        )
    }

    @Test
    fun `POST 请求附带正确 Method 参与签名并写入 Token 与 Channel`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(
            captured = lastRequest,
            token = "jwt-mock-token",
            channel = "3",
        )

        client.post("https://picaapi.picacomic.com/comics/like")

        val request = assertNotNull(lastRequest.get())
        assertEquals("jwt-mock-token", request.headers[HttpHeaders.Authorization])
        assertEquals("3", request.headers["app-channel"])
        assertHeadersAndSignature(
            request = request,
            expectedPath = "comics/like",
            expectedMethod = "POST",
        )
    }

    private fun createTestClient(
        captured: AtomicReference<HttpRequestData>,
        token: String? = null,
        channel: String = "1",
    ): HttpClient = HttpClient(MockEngine { req ->
        captured.set(req)
        respondOk()
    }) {
        bikaAuth {
            token { token }
            channel { channel }
        }
    }

    private fun assertHeadersAndSignature(
        request: HttpRequestData,
        expectedPath: String,
        expectedMethod: String,
    ) {
        val headers = request.headers
        val config = BikaAuthConfig()
        val apiKey = config.apiKey
        val secretKey = config.secretKey

        assertEquals(apiKey, headers["api-key"])
        assertEquals("okhttp/3.8.1", headers["User-Agent"])
        assertEquals("application/vnd.picacomic.com.v1+json", headers["accept"])

        val time = assertNotNull(headers["time"])
        val nonce = assertNotNull(headers["nonce"])
        val actualSig = assertNotNull(headers["signature"])

        // 验证签名算法数学一致性
        val rawData = (expectedPath.lowercase() + time + nonce + expectedMethod + apiKey).lowercase()
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        }
        val expectedSig = mac.doFinal(rawData.toByteArray(Charsets.UTF_8)).toHexString()

        assertEquals(expectedSig, actualSig)
    }
}
