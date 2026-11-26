package com.shizq.bika.feature.reader.impl.util.preload

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.decode.BlackholeDecoder
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.Dimension
import coil3.size.Size
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.core.network.plugin.DomainFallbackInterceptor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalCoilApi::class)
class ChapterPagePreloadProviderTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `preload request enables memory and disk cache with matching keys`() = runTest {
        val url = "https://s3.picacomic.com/static/page1.jpg"
        val provider = ChapterPagePreloadProvider(context)

        val request = provider.getPreloadRequest(ChapterPage(id = "p1", url = url))

        assertEquals(CachePolicy.ENABLED, request.memoryCachePolicy)
        assertEquals(CachePolicy.ENABLED, request.diskCachePolicy)
        assertEquals(url, request.memoryCacheKey)
        assertEquals(url, request.diskCacheKey)
        assertNotEquals(BlackholeDecoder.Factory::class, request.decoderFactory?.let { it::class })

        val resolvedSize = request.sizeResolver.size()
        assertIs<Dimension.Pixels>(resolvedSize.width)
        assertTrue((resolvedSize.width as Dimension.Pixels).px >= 1080)
        assertEquals(Dimension.Undefined, resolvedSize.height)
    }

    @Test
    fun `slow preload main request times out and sequentially falls back to mirror`() = runTest {
        val primaryHost = "s3.picacomic.com"
        val url = "https://$primaryHost/static/page2.jpg"
        val provider = ChapterPagePreloadProvider(context)
        val preloadRequest = provider.getPreloadRequest(ChapterPage(id = "p2", url = url))

        val interceptor = DomainFallbackInterceptor(
            slowMainThresholdMs = 100L,
            slowPreloadThresholdMs = 200L,
        )

        val attemptedHosts = mutableListOf<String>()
        var concurrentMirrors = 0
        var peakMirrorConcurrency = 0

        val result = interceptor.intercept(
            FakeChain(preloadRequest) { req ->
                val host = URI(req.data as String).host
                attemptedHosts += host
                if (host == primaryHost) {
                    // 模拟主域名卡死不返回
                    delay(10_000L)
                    SuccessResult(ColorImage(), req, DataSource.NETWORK)
                } else {
                    concurrentMirrors++
                    peakMirrorConcurrency = maxOf(peakMirrorConcurrency, concurrentMirrors)
                    try {
                        delay(50L)
                        if (host == "s2.picacomic.com") {
                            SuccessResult(ColorImage(), req, DataSource.NETWORK)
                        } else {
                            ErrorResult(null, req, IllegalStateException("mirror down"))
                        }
                    } finally {
                        concurrentMirrors--
                    }
                }
            }
        )

        assertIs<SuccessResult>(result)
        assertTrue(testScheduler.currentTime < 2_000L, "预加载应快速超时切换，而非死等主请求")
        assertEquals(1, peakMirrorConcurrency, "预加载备用域名必须保持串行，避免流量放大")
        assertTrue(attemptedHosts.first() == primaryHost)
    }

    private class FakeChain(
        override val request: ImageRequest,
        private val handler: suspend (ImageRequest) -> ImageResult,
    ) : Interceptor.Chain {
        override val size: Size = Size.ORIGINAL

        override fun withRequest(request: ImageRequest): Interceptor.Chain =
            FakeChain(request, handler)

        override fun withSize(size: Size): Interceptor.Chain = this

        override suspend fun proceed(): ImageResult = handler(request)
    }
}
