package com.shizq.bika.core.ui

import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 退避曲线原先在三处各写了一遍（章节分页重试、阅读器单页重试、RetryableAsyncImage），
 * 并且已经漂移：两处对位移量做了钳制，第三处没做——`2000L shl 32` 会绕回 2000，
 * 无限退避退化成 2s 一次的轮询，是最容易在 code review 里看漏的一类缺陷
 * （`coerceAtMost(30_000L)` 明明在，看着像已经封顶了）。
 *
 * 抽成纯函数后这条曲线终于可测，这里把它钉死。
 */
class BackoffDelayTest {

    @Test
    fun `前五次按 2 的幂递增`() {
        assertEquals(2_000L, backoffDelayMillis(0))
        assertEquals(4_000L, backoffDelayMillis(1))
        assertEquals(8_000L, backoffDelayMillis(2))
        assertEquals(16_000L, backoffDelayMillis(3))
    }

    @Test
    fun `第五次起封顶在 30 秒`() {
        assertEquals(30_000L, backoffDelayMillis(4))
        assertEquals(30_000L, backoffDelayMillis(5))
        assertEquals(30_000L, backoffDelayMillis(50))
    }

    /**
     * 回归：`shl` 的右操作数按 mod 32 取模。先移再 coerce 时 attempt=32
     * 会算出 2000L（绕回），曲线失效。
     */
    @Test
    fun `位移量绕回不会让退避失效`() {
        assertEquals(30_000L, backoffDelayMillis(32))
        assertEquals(30_000L, backoffDelayMillis(33))
        assertEquals(30_000L, backoffDelayMillis(64))
        assertEquals(30_000L, backoffDelayMillis(Int.MAX_VALUE))
    }

    @Test
    fun `负数不产生异常间隔`() {
        assertEquals(2_000L, backoffDelayMillis(-1))
        assertEquals(2_000L, backoffDelayMillis(Int.MIN_VALUE))
    }

    @Test
    fun `明确错误分类`() {
        assertEquals(ImageRetryDecision.Retry, IOException().retryDecision())
        assertEquals(ImageRetryDecision.Stop, IllegalArgumentException().retryDecision())
        assertEquals(ImageRetryDecision.Stop, null.retryDecision())
    }
    @Test
    fun `曲线单调不减且恒为正`() {
        var previous = 0L
        for (attempt in 0..40) {
            val current = backoffDelayMillis(attempt)
            assertTrue(current > 0L, "attempt=$attempt 间隔必须为正，实际 $current")
            assertTrue(current >= previous, "attempt=$attempt 间隔回退了: $previous -> $current")
            assertTrue(current <= 30_000L, "attempt=$attempt 超过封顶: $current")
            previous = current
        }
    }
}
