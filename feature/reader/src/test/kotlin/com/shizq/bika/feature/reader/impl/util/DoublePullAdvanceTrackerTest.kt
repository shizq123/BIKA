package com.shizq.bika.feature.reader.impl.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DoublePullAdvanceTrackerTest {

    @Test
    fun `first pull prompts last page`() {
        var currentTime = 1000L
        val tracker = DoublePullAdvanceTracker(
            timeoutMillis = 3000L,
            timeProvider = { currentTime },
        )

        val result = tracker.onPull()

        assertEquals(DoublePullResult.PromptLastPage, result)
        assertEquals(1000L, tracker.lastPullTime)
    }

    @Test
    fun `second pull within timeout triggers advance and resets state`() {
        var currentTime = 1000L
        val tracker = DoublePullAdvanceTracker(
            timeoutMillis = 3000L,
            timeProvider = { currentTime },
        )

        tracker.onPull()

        // 1.5 秒后再次下拉
        currentTime += 1500L
        val secondResult = tracker.onPull()

        assertEquals(DoublePullResult.TriggerAdvance, secondResult)
        assertEquals(0L, tracker.lastPullTime)

        // 跳转后若再次下拉，应重新当作第一次
        currentTime += 500L
        val thirdResult = tracker.onPull()
        assertEquals(DoublePullResult.PromptLastPage, thirdResult)
        assertEquals(3000L, tracker.lastPullTime)
    }

    @Test
    fun `second pull after timeout resets and prompts last page again`() {
        var currentTime = 1000L
        val tracker = DoublePullAdvanceTracker(
            timeoutMillis = 3000L,
            timeProvider = { currentTime },
        )

        tracker.onPull()

        // 超过 3 秒（例如 3.5 秒后）再次下拉
        currentTime += 3500L
        val result = tracker.onPull()

        assertEquals(DoublePullResult.PromptLastPage, result)
        assertEquals(4500L, tracker.lastPullTime)
    }

    @Test
    fun `reset clears tracker state`() {
        var currentTime = 1000L
        val tracker = DoublePullAdvanceTracker(
            timeoutMillis = 3000L,
            timeProvider = { currentTime },
        )

        tracker.onPull()
        assertEquals(1000L, tracker.lastPullTime)

        tracker.reset()
        assertEquals(0L, tracker.lastPullTime)

        // 重置后立即下拉，仍应作为第一次处理
        currentTime += 100L
        val result = tracker.onPull()
        assertEquals(DoublePullResult.PromptLastPage, result)
        assertEquals(1100L, tracker.lastPullTime)
    }
}
