package com.shizq.bika.feature.reader.impl.util.preload

import org.junit.Test
import kotlin.test.assertEquals

/**
 * 翻页速率 -> 预载张数的自适应。
 *
 * [AdaptivePreloadTracker.onPageChanged] 的时间戳由调用方注入，正是为了能在这里
 * 用固定序列驱动。调用方 [rememberAdaptivePreloadCount] 传的是
 * `SystemClock.elapsedRealtime()`（开机时长）而不是墙钟：墙钟会因用户改时间或
 * NTP 校正回跳，算出负间隔后被 [AdaptivePreloadPolicy.isValidInterval] 剔除，
 * 自适应静默失效。最后一个用例把这个场景固定下来。
 */
class AdaptivePreloadTrackerTest {

    private val policy = AdaptivePreloadPolicy()

    @Test
    fun `样本不足时维持基准张数`() {
        val tracker = AdaptivePreloadTracker(policy)

        // 首次翻页没有前序时间戳，之后两次也凑不满 sampleWindowSize=3
        assertEquals(4, tracker.onPageChanged(1_000, baselineCount = 4))
        assertEquals(4, tracker.onPageChanged(2_000, baselineCount = 4))
        assertEquals(4, tracker.onPageChanged(3_000, baselineCount = 4))
    }

    @Test
    fun `快速扫读时提高预载张数`() {
        val tracker = AdaptivePreloadTracker(policy)
        var now = 0L

        // 每 500ms 翻一页，平均远低于 fastReadingThreshold=1500
        repeat(5) {
            now += 500
            tracker.onPageChanged(now, baselineCount = 4)
        }

        now += 500
        assertEquals(
            policy.fastReadingPreloadCount,
            tracker.onPageChanged(now, baselineCount = 4),
        )
    }

    @Test
    fun `慢速精读时降低预载张数`() {
        val tracker = AdaptivePreloadTracker(policy)
        var now = 0L

        // 每 5s 翻一页，高于 slowReadingThreshold=3200 且仍在有效区间内
        repeat(5) {
            now += 5_000
            tracker.onPageChanged(now, baselineCount = 8)
        }

        now += 5_000
        assertEquals(
            policy.slowReadingPreloadCount,
            tracker.onPageChanged(now, baselineCount = 8),
        )
    }

    @Test
    fun `中速阅读维持基准张数`() {
        val tracker = AdaptivePreloadTracker(policy)
        var now = 0L

        // 2s 落在 fast(1500) 与 slow(3200) 之间
        repeat(6) {
            now += 2_000
            tracker.onPageChanged(now, baselineCount = 5)
        }

        now += 2_000
        assertEquals(5, tracker.onPageChanged(now, baselineCount = 5))
    }

    @Test
    fun `关闭预载时始终返回 0`() {
        val tracker = AdaptivePreloadTracker(policy)
        var now = 0L

        // 即使速率判定为快速扫读，baselineCount=0 也不能被抬成非 0，
        // 否则用户明确关掉的预载会偷偷复活。
        repeat(6) {
            now += 300
            assertEquals(0, tracker.onPageChanged(now, baselineCount = 0))
        }
    }

    @Test
    fun `过短的间隔被剔除不参与判定`() {
        val tracker = AdaptivePreloadTracker(policy)
        var now = 0L

        // 50ms < minValidInterval=100，属于连点/程序滚动，全部无效，
        // 样本数始终为 0，应一直维持基准值。
        repeat(8) {
            now += 50
            assertEquals(4, tracker.onPageChanged(now, baselineCount = 4))
        }
    }

    @Test
    fun `过长的间隔被剔除不参与判定`() {
        val tracker = AdaptivePreloadTracker(policy)
        var now = 0L

        // 30s > maxValidInterval=10_000，属于放置一段时间后回来继续读。
        repeat(8) {
            now += 30_000
            assertEquals(4, tracker.onPageChanged(now, baselineCount = 4))
        }
    }

    @Test
    fun `滑动窗口只保留最近的样本`() {
        val tracker = AdaptivePreloadTracker(policy)
        var now = 0L

        // 先制造一批慢速样本
        repeat(4) {
            now += 5_000
            tracker.onPageChanged(now, baselineCount = 4)
        }
        assertEquals(
            policy.slowReadingPreloadCount,
            tracker.onPageChanged(now + 5_000, baselineCount = 4),
        )
        now += 5_000

        // 再连续快速翻页，窗口大小为 3，足量新样本应把旧的慢速样本挤出去
        repeat(policy.sampleWindowSize) {
            now += 400
            tracker.onPageChanged(now, baselineCount = 4)
        }
        now += 400
        assertEquals(
            policy.fastReadingPreloadCount,
            tracker.onPageChanged(now, baselineCount = 4),
            "窗口未及时淘汰旧样本，速率变化后自适应会滞后",
        )
    }

    @Test
    fun `时钟回跳产生的负间隔被剔除`() {
        val tracker = AdaptivePreloadTracker(policy)

        // 先攒满快速扫读的样本
        var now = 0L
        repeat(policy.sampleWindowSize + 1) {
            now += 400
            tracker.onPageChanged(now, baselineCount = 4)
        }

        // 时钟回跳：负间隔无效，不得污染窗口，也不能让结果跳成基准值
        val afterJump = tracker.onPageChanged(now - 60_000, baselineCount = 4)
        assertEquals(
            policy.fastReadingPreloadCount,
            afterJump,
            "负间隔应被剔除，已有样本仍然有效",
        )
    }
}
