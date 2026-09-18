package com.shizq.bika.core.network.plugin

import coil3.annotation.ExperimentalCoilApi
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalCoilApi::class)
class ImageDownloadCoordinatorTest {
    @Test
    fun `visible request uses the cache filled by the in flight preload`() = runTest {
        val coordinator = ImageDownloadCoordinator()
        var cached = false
        var downloads = 0
        val result = imageResult()
        suspend fun fetch() = coordinator.apply("page") {
            if (!cached) {
                downloads++
                delay(100)
                cached = true
            }
            result
        }
        val preload = async { fetch() }
        val visible = async { fetch() }
        advanceUntilIdle()

        assertEquals(result, preload.await())
        assertEquals(result, visible.await())
        assertEquals(1, downloads)
        result.source.close()
    }

    @Test
    fun `mirror can rescue an image even while primary owns the same cache key`() = runTest {
        val coordinator = ImageDownloadCoordinator()
        val primary = backgroundScope.async {
            coordinator.apply("page") { awaitCancellation() }
        }
        runCurrent()
        val result = imageResult()
        val fallback = withContext(FallbackMarker()) {
            coordinator.apply("page") { result }
        }

        assertEquals(result, fallback)
        assertTrue(primary.isActive)
        assertEquals(0L, testScheduler.currentTime)
        primary.cancel()
        result.source.close()
    }

    @Test
    fun `different pages download concurrently`() = runTest {
        val coordinator = KeyedRequestCoordinator()
        val first = async { coordinator.withKey("one") { delay(100); 1 } }
        val second = async { coordinator.withKey("two") { delay(100); 2 } }
        assertEquals(1, first.await())
        assertEquals(2, second.await())
        assertEquals(100L, testScheduler.currentTime)
    }

    @Test
    fun `cancelling a waiter does not block the next request`() = runTest {
        val coordinator = KeyedRequestCoordinator()
        val first = async { coordinator.withKey("page") { delay(100); 1 } }
        val cancelled = async { coordinator.withKey("page") { error("Must not run") } }
        runCurrent()
        cancelled.cancel()
        val next = async { coordinator.withKey("page") { 2 } }

        assertEquals(1, first.await())
        assertEquals(2, next.await())
        assertEquals(100L, testScheduler.currentTime)
    }

    @Test
    fun `cancelling the owner lets a visible request take over`() = runTest {
        val coordinator = KeyedRequestCoordinator()
        val first = backgroundScope.async {
            coordinator.withKey("page") { awaitCancellation() }
        }
        runCurrent()
        val next = async { coordinator.withKey("page") { 2 } }
        runCurrent()
        first.cancel()
        assertEquals(2, next.await())
    }

    private fun imageResult() = SourceFetchResult(
        source = ImageSource(Buffer(), FileSystem.SYSTEM),
        mimeType = "image/jpeg",
        dataSource = DataSource.NETWORK,
    )
}
