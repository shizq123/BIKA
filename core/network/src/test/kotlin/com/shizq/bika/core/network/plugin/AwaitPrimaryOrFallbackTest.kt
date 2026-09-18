package com.shizq.bika.core.network.plugin

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AwaitPrimaryOrFallbackTest {
    @Test
    fun `primary success is displayed without waiting for slow mirrors`() = runTest {
        val primary = async { delay(100); "primary" }
        val fallback = async<String?> { delay(3000); "mirror" }

        assertEquals("primary", awaitPrimaryOrFallback(primary, fallback) { true })
        assertEquals(100L, testScheduler.currentTime)
        assertTrue(fallback.isCancelled)
    }

    @Test
    fun `successful mirror cancels the slower primary`() = runTest {
        val primary = async { delay(3000); "primary" }
        val fallback = async<String?> { delay(100); "mirror" }

        assertEquals("mirror", awaitPrimaryOrFallback(primary, fallback) { true })
        assertEquals(100L, testScheduler.currentTime)
        assertTrue(primary.isCancelled)
    }

    @Test
    fun `primary failure still allows a mirror to succeed`() = runTest {
        val primary = async { "failed" }
        val fallback = async<String?> { delay(100); "mirror" }

        assertEquals("mirror", awaitPrimaryOrFallback(primary, fallback) { it != "failed" })
        assertEquals(100L, testScheduler.currentTime)
    }

    @Test
    fun `exhausted mirrors do not cancel a still useful primary`() = runTest {
        val primary = async { delay(100); "primary" }
        val fallback = async<String?> { null }

        assertEquals("primary", awaitPrimaryOrFallback(primary, fallback) { true })
        assertEquals(100L, testScheduler.currentTime)
    }

    @Test
    fun `all failures preserve the original error`() = runTest {
        val primary = async { "original error" }
        val fallback = async<String?> { delay(100); null }

        assertEquals("original error", awaitPrimaryOrFallback(primary, fallback) { false })
    }
}
