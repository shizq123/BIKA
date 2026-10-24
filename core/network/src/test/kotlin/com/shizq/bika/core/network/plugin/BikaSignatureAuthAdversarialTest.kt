package com.shizq.bika.core.network.plugin

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.fullPath
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * [BikaSignatureAuth] 对抗性与极端 URL 边界测试套件。
 * 覆盖：空路径、多重斜杠、特殊字符与转义字符、复杂多层 Query、非标端口、纯 IPv4/IPv6、路径穿越/注入防范。
 */
class BikaSignatureAuthAdversarialTest {

    // ── 1. 空路径边界检验 (Empty Path) ───────────────────────────────────

    @Test
    fun `根路径单斜杠请求不会导致下标越界且相对路径为空字符串`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(lastRequest)

        client.get("https://picaapi.picacomic.com/")

        val request = assertNotNull(lastRequest.get())
        // encodedPath 为 "/"，removePrefix("/") 得到 ""
        assertHeadersAndSignature(
            request = request,
            expectedPath = "",
            expectedMethod = "GET",
        )
    }

    @Test
    fun `无斜杠根地址请求不会崩溃且能正确生成签名`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(lastRequest)

        client.get("https://picaapi.picacomic.com")

        val request = assertNotNull(lastRequest.get())
        assertHeadersAndSignature(
            request = request,
            expectedPath = "",
            expectedMethod = "GET",
        )
    }

    // ── 2. 多重斜杠边界检验 (Multiple Slashes) ───────────────────────────

    @Test
    fun `连续多重斜杠路径能被安全处理不崩溃`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(lastRequest)

        // 输入包含连续双斜杠与三斜杠
        client.get("https://picaapi.picacomic.com//comics///123")

        val request = assertNotNull(lastRequest.get())
        // Ktor 的 encodedPath 保留原始路径 "//comics///123"，removePrefix("/") 去掉首个 "/" 剩下 "/comics///123"
        val expected = request.url.fullPath.removePrefix("/").lowercase()
        assertHeadersAndSignature(
            request = request,
            expectedPath = expected,
            expectedMethod = "GET",
        )
    }

    // ── 3. 特殊字符、转义字符与大小写归一化检验 ──────────────────────────

    @Test
    fun `大小写混合路径在签名计算时被完整转换为小写`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(lastRequest)

        client.get("https://picaapi.picacomic.com/Auth/Sign-In")

        val request = assertNotNull(lastRequest.get())
        assertHeadersAndSignature(
            request = request,
            expectedPath = "auth/sign-in",
            expectedMethod = "GET",
        )
    }

    @Test
    fun `包含 URL 转义字符与空格的路径签名正常提取`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(lastRequest)

        client.get("https://picaapi.picacomic.com/comics/my%20special%20comic")

        val request = assertNotNull(lastRequest.get())
        assertHeadersAndSignature(
            request = request,
            expectedPath = "comics/my%20special%20comic",
            expectedMethod = "GET",
        )
    }

    @Test
    fun `中文 URL 编码路径安全签名`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(lastRequest)

        // 哔哩哔哩 URL 编码: %E5%93%94%E5%93%A9%E5%93%94%E5%93%A9 -> 小写 %e5%93%94%e5%93%a9%e5%93%94%e5%93%a9
        client.get("https://picaapi.picacomic.com/comics/%E5%93%94%E5%93%A9%E5%93%94%E5%93%A9")

        val request = assertNotNull(lastRequest.get())
        assertHeadersAndSignature(
            request = request,
            expectedPath = "comics/%e5%93%94%e5%93%a9%e5%93%94%e5%93%a9",
            expectedMethod = "GET",
        )
    }

    // ── 4. 多层与复杂 Query 参数参与签名计算检验 ─────────────────────────

    @Test
    fun `极端复杂多层 Query 参数完整参与签名计算`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(lastRequest)

        client.get("https://picaapi.picacomic.com/comics") {
            parameter("page", 1)
            parameter("c", "tag")
            parameter("sort", "dd")
            parameter("extra", "a=1&b=2")
            parameter("empty", "")
            parameter("special", "!@#\$%^&*()_+")
        }

        val request = assertNotNull(lastRequest.get())
        val expected = request.url.fullPath.removePrefix("/").lowercase()
        assertHeadersAndSignature(
            request = request,
            expectedPath = expected,
            expectedMethod = "GET",
        )
    }

    // ── 5. 非标准端口号检验 (Non-Standard Ports) ─────────────────────────

    @Test
    fun `非标端口 8443 与 8080 下端口号不泄露至签名路径中`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(lastRequest)

        client.get("https://mirror.picacomic.internal:8443/comics/123/pages")

        val request = assertNotNull(lastRequest.get())
        assertHeadersAndSignature(
            request = request,
            expectedPath = "comics/123/pages",
            expectedMethod = "GET",
        )

        client.get("http://mirror.picacomic.internal:8080/auth/sign-in")
        val req2 = assertNotNull(lastRequest.get())
        assertHeadersAndSignature(
            request = req2,
            expectedPath = "auth/sign-in",
            expectedMethod = "GET",
        )
    }

    // ── 6. 纯 IP 地址场景检验 (IPv4 与 IPv6) ────────────────────────────

    @Test
    fun `纯 IPv4 地址访问时 IP 不泄漏到签名路径中`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(lastRequest)

        client.get("http://68.183.234.72/comics")

        val request = assertNotNull(lastRequest.get())
        assertHeadersAndSignature(
            request = request,
            expectedPath = "comics",
            expectedMethod = "GET",
        )
    }

    @Test
    fun `纯 IPv4 地址带端口访问时签名路径完全纯净`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(lastRequest)

        client.get("http://192.168.1.100:8888/users/profile")

        val request = assertNotNull(lastRequest.get())
        assertHeadersAndSignature(
            request = request,
            expectedPath = "users/profile",
            expectedMethod = "GET",
        )
    }

    @Test
    fun `纯 IPv6 回环地址访问时方括号与端口不泄漏到签名路径中`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(lastRequest)

        client.get("http://[::1]:8080/keywords")

        val request = assertNotNull(lastRequest.get())
        assertHeadersAndSignature(
            request = request,
            expectedPath = "keywords",
            expectedMethod = "GET",
        )
    }

    // ── 7. 路径遍历与注入防范 (Path Traversal / Injection) ───────────────

    @Test
    fun `相对路径中包含点和路径穿越符号被安全处理`() = runTest {
        val lastRequest = AtomicReference<HttpRequestData>()
        val client = createTestClient(lastRequest)

        // 模拟攻击者尝试路径穿越
        client.get("https://picaapi.picacomic.com/comics/../users/punch-in")

        val request = assertNotNull(lastRequest.get())
        // Ktor 对 URL 会规范化或保留 encodedPath，无论如何均不抛异常，且能产出确定性签名
        val actualPath = request.url.fullPath.removePrefix("/").lowercase()
        assertHeadersAndSignature(
            request = request,
            expectedPath = actualPath,
            expectedMethod = "GET",
        )
    }

    // ── 辅助方法 ────────────────────────────────────────────────────────

    private fun createTestClient(
        captured: AtomicReference<HttpRequestData>,
    ): HttpClient = HttpClient(MockEngine { req ->
        captured.set(req)
        respondOk()
    }) {
        bikaAuth()
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

        val rawData = (expectedPath.lowercase() + time + nonce + expectedMethod + apiKey).lowercase()
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        }
        val expectedSig = mac.doFinal(rawData.toByteArray(Charsets.UTF_8)).toHexString()

        assertEquals(expectedSig, actualSig, "签名计算值不匹配: path=$expectedPath, method=$expectedMethod")
    }
}
