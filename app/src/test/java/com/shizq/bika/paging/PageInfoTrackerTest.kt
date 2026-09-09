package com.shizq.bika.paging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** 只实现上报契约的替身，不涉及 Paging 框架，因此无需协程或 Android 环境。 */
private class FakeReportingSource : PageInfoReporting {
    override var onPageInfoLoaded: ((totalPages: Int, totalCount: Int) -> Unit)? = null

    /** 模拟一次加载完成后的旁路上报。 */
    fun report(totalPages: Int, totalCount: Int = 0) {
        onPageInfoLoaded?.invoke(totalPages, totalCount)
    }
}

class PageInfoTrackerTest {

    @Test
    fun `首次加载完成前总页数为 1`() {
        val tracker = PageInfoTracker()

        assertEquals(1, tracker.totalPages.value)
    }

    @Test
    fun `track 会装配回调`() {
        val tracker = PageInfoTracker()
        val source = FakeReportingSource()

        tracker.track(source)

        assertNotNull(source.onPageInfoLoaded)
    }

    @Test
    fun `track 原样返回传入的数据源`() {
        val tracker = PageInfoTracker()
        val source = FakeReportingSource()

        // 返回值需能直接作为 Pager 工厂的产物，不能是包装对象
        assertEquals(source, tracker.track(source))
    }

    @Test
    fun `上报的总页数会写入 totalPages`() {
        val tracker = PageInfoTracker()
        val source = FakeReportingSource().also { tracker.track(it) }

        source.report(totalPages = 7)

        assertEquals(7, tracker.totalPages.value)
    }

    @Test
    fun `同一数据源多次上报以最后一次为准`() {
        val tracker = PageInfoTracker()
        val source = FakeReportingSource().also { tracker.track(it) }

        source.report(totalPages = 7)
        source.report(totalPages = 9)

        assertEquals(9, tracker.totalPages.value)
    }

    @Test
    fun `服务端返回 0 页时收敛为 1`() {
        val tracker = PageInfoTracker()
        val source = FakeReportingSource().also { tracker.track(it) }

        // 结果集为空时服务端返回 0，若原样写入，UI 会渲染出「1 ~ 0」这种无法通过校验的区间
        source.report(totalPages = 0)

        assertEquals(1, tracker.totalPages.value)
    }

    @Test
    fun `负数页数同样收敛为 1`() {
        val tracker = PageInfoTracker()
        val source = FakeReportingSource().also { tracker.track(it) }

        source.report(totalPages = -3)

        assertEquals(1, tracker.totalPages.value)
    }

    @Test
    fun `过期数据源的上报被丢弃`() {
        val tracker = PageInfoTracker()
        val stale = FakeReportingSource().also { tracker.track(it) }
        val fresh = FakeReportingSource().also { tracker.track(it) }

        fresh.report(totalPages = 3)
        // 上一代数据源已被 flatMapLatest 取消，但它的网络响应可能已在途，回调仍会执行一次
        stale.report(totalPages = 99)

        assertEquals(3, tracker.totalPages.value)
    }

    @Test
    fun `过期数据源先上报也不影响最新一代`() {
        val tracker = PageInfoTracker()
        val stale = FakeReportingSource().also { tracker.track(it) }
        tracker.track(FakeReportingSource())

        // 装配新一代后，旧一代的上报无论早于还是晚于新一代都应被忽略
        stale.report(totalPages = 99)

        assertEquals(1, tracker.totalPages.value)
    }

    @Test
    fun `最新一代在过期上报之后仍能写入`() {
        val tracker = PageInfoTracker()
        val stale = FakeReportingSource().also { tracker.track(it) }
        val fresh = FakeReportingSource().also { tracker.track(it) }

        stale.report(totalPages = 99)
        fresh.report(totalPages = 4)

        // 屏蔽过期上报不能把最新一代一起屏蔽掉
        assertEquals(4, tracker.totalPages.value)
    }

    @Test
    fun `重建同一数据源实例时最新一代仍然生效`() {
        val tracker = PageInfoTracker()
        val source = FakeReportingSource()

        // Paging 失效后工厂会再次执行，可能对同一实例重复装配
        tracker.track(source)
        tracker.track(source)
        source.report(totalPages = 5)

        assertEquals(5, tracker.totalPages.value)
    }

    @Test
    fun `连续三代只有最后一代生效`() {
        val tracker = PageInfoTracker()
        val first = FakeReportingSource().also { tracker.track(it) }
        val second = FakeReportingSource().also { tracker.track(it) }
        val third = FakeReportingSource().also { tracker.track(it) }

        third.report(totalPages = 6)
        second.report(totalPages = 88)
        first.report(totalPages = 99)

        assertEquals(6, tracker.totalPages.value)
    }
}
