package com.shizq.bika.feature.reader.impl.util.preload

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PreloadQueueTest {
    @Test
    fun `a page becoming visible finishes its existing download without a restart`() = runTest {
        val f = Fixture(this)
        f.queue.update(listOf(1, 2, 3))
        runCurrent()
        f.queue.update(listOf(2, 3, 4), retainRunning = setOf(1))
        runCurrent()
        assertEquals(listOf(1, 2), f.started)
        assertTrue(f.cancelled.isEmpty())

        f.finish(1)
        runCurrent()
        assertEquals(listOf(1, 2, 3), f.started)
        assertEquals(2, f.peakConcurrency)
    }

    @Test
    fun `a swipe before queued jobs start does not leave their slots occupied`() = runTest {
        val f = Fixture(this)
        f.queue.update(listOf(1, 2, 3))
        f.queue.update(listOf(20, 21, 22))
        runCurrent()
        assertEquals(listOf(20, 21), f.started)
        f.finish(20)
        runCurrent()
        assertEquals(listOf(20, 21, 22), f.started)
    }

    @Test
    fun `nearest pages start first and background concurrency stays bounded`() = runTest {
        val f = Fixture(this)
        f.queue.update(listOf(1, 2, 3, 4))
        runCurrent()
        assertEquals(listOf(1, 2), f.started)

        f.finish(1)
        runCurrent()
        assertEquals(listOf(1, 2, 3), f.started)
        assertEquals(2, f.peakConcurrency)
    }

    @Test
    fun `overlapping windows keep in flight downloads and cancel stale pages`() = runTest {
        val f = Fixture(this)
        f.queue.update(listOf(1, 2, 3))
        runCurrent()
        f.queue.update(listOf(2, 3, 4))
        runCurrent()

        assertEquals(listOf(1, 2, 3), f.started)
        assertEquals(listOf(1), f.cancelled)
        f.finish(2)
        runCurrent()
        assertEquals(listOf(1, 2, 3, 4), f.started)
        assertEquals(2, f.peakConcurrency)
    }

    @Test
    fun `layout updates neither duplicate downloads nor restart completed pages`() = runTest {
        val f = Fixture(this)
        f.queue.update(listOf(1, 1, 2))
        runCurrent()
        f.queue.update(listOf(1, 2))
        f.finish(1)
        runCurrent()
        f.queue.update(listOf(1, 2))
        runCurrent()

        assertEquals(listOf(1, 2), f.started)
        assertTrue(f.cancelled.isEmpty())
    }

    @Test
    fun `jump discards pending old pages before starting the new window`() = runTest {
        val f = Fixture(this)
        f.queue.update(listOf(1, 2, 3, 4))
        runCurrent()
        f.queue.update(listOf(20, 21, 22))
        runCurrent()

        assertEquals(listOf(1, 2, 20, 21), f.started)
        assertEquals(setOf(1, 2), f.cancelled.toSet())
        assertEquals(2, f.peakConcurrency)
    }

    @Test
    fun `disabling preloads cancels active work and allows a later fresh window`() = runTest {
        val f = Fixture(this)
        f.queue.update(listOf(1, 2, 3))
        runCurrent()
        f.queue.update(emptyList())
        runCurrent()
        assertEquals(listOf(1, 2), f.started)
        assertEquals(setOf(1, 2), f.cancelled.toSet())

        f.queue.update(listOf(4, 5))
        runCurrent()
        assertEquals(listOf(1, 2, 4, 5), f.started)
    }

    @Test
    fun `leaving the reader cancels everything and cannot restart queued work`() = runTest {
        val f = Fixture(this)
        f.queue.update(listOf(1, 2, 3))
        runCurrent()
        f.queue.close()
        f.queue.update(listOf(8, 9))
        runCurrent()

        assertEquals(listOf(1, 2), f.started)
        assertEquals(setOf(1, 2), f.cancelled.toSet())
    }

    @Test
    fun `rapid reversal can retry a page whose cancellation has not completed yet`() = runTest {
        val f = Fixture(this)
        f.queue.update(listOf(1, 2))
        runCurrent()
        f.queue.update(emptyList())
        f.queue.update(listOf(1, 2))
        runCurrent()

        assertEquals(listOf(1, 2, 1, 2), f.started)
        assertEquals(2, f.peakConcurrency)
    }

    private class Fixture(scope: TestScope) {
        val started = mutableListOf<Int>()
        val cancelled = mutableListOf<Int>()
        private val completions = mutableMapOf<Int, CompletableDeferred<Unit>>()
        private var concurrency = 0
        var peakConcurrency = 0
            private set

        val queue = PreloadQueue(
            scope = scope.backgroundScope,
            keyOf = { page: Int -> page },
            execute = { page ->
                started += page
                concurrency++
                peakConcurrency = maxOf(peakConcurrency, concurrency)
                try {
                    completions.getOrPut(page) { CompletableDeferred() }.await()
                } finally {
                    concurrency--
                    if (!currentCoroutineContext().isActive) cancelled += page
                }
            },
        )

        fun finish(page: Int) {
            completions.getValue(page).complete(Unit)
        }
    }
}
