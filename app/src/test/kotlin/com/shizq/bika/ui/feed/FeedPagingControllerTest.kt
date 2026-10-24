package com.shizq.bika.ui.feed

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.paging.PageInfoReporting
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FeedPagingControllerTest {
    @Test
    fun `initialization waits for configuration and ignores early page changes`() {
        val controller = FeedPagingController(supportsPagination = true)

        assertNull(controller.state.value.request)
        assertNull(controller.state.value.totalPages)
        controller.changePage(2)
        controller.changePage(1)
        assertNull(controller.state.value.request)
        assertNull(controller.state.value.totalPages)

        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)

        val request = controller.request()
        assertEquals(SortOrder.NEWEST, request.sort)
        assertFalse(request.continuousScrollEnabled)
        assertEquals(1, request.page)
        assertNull(controller.state.value.totalPages)
    }

    @Test
    fun `repeating the default configuration keeps the request and loaded metadata`() {
        val controller = FeedPagingController(supportsPagination = true)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = true)
        val source = controller.trackedSource()
        source.report(pages = 6)
        val loaded = controller.state.value

        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = true)

        assertEquals(loaded, controller.state.value)
        source.report(pages = 7)
        assertEquals(7, controller.state.value.totalPages)
        assertEquals(loaded.request, controller.request())
    }

    @Test
    fun `repeating manual configuration preserves the selected page and active callback`() {
        val controller = FeedPagingController(supportsPagination = true)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)
        controller.trackedSource().report(pages = 6)
        controller.changePage(3)
        val source = controller.trackedSource()
        source.report(pages = 6)
        val selected = controller.state.value

        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)

        assertEquals(selected, controller.state.value)
        source.report(pages = 7)
        assertEquals(7, controller.state.value.totalPages)
        assertEquals(selected.request, controller.request())
    }

    @Test
    fun `switching scrolling mode starts a new request on page one in either direction`() {
        val controller = FeedPagingController(supportsPagination = true)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)
        controller.trackedSource().report(pages = 6)
        controller.changePage(3)
        val manual = controller.request()

        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = true)

        val continuous = controller.request()
        assertTrue(continuous.continuousScrollEnabled)
        assertEquals(SortOrder.NEWEST, continuous.sort)
        assertEquals(1, continuous.page)
        assertTrue(continuous.revision > manual.revision)
        assertNull(controller.state.value.totalPages)

        controller.trackedSource().report(pages = 8)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)

        val manualAgain = controller.request()
        assertFalse(manualAgain.continuousScrollEnabled)
        assertEquals(SortOrder.NEWEST, manualAgain.sort)
        assertEquals(1, manualAgain.page)
        assertTrue(manualAgain.revision > continuous.revision)
        assertNull(controller.state.value.totalPages)
    }

    @Test
    fun `changing sort returns to page one and clears totals from the previous order`() {
        val controller = FeedPagingController(supportsPagination = true)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)
        controller.trackedSource().report(pages = 6)
        controller.changePage(4)
        val previous = controller.request()

        controller.configure(SortOrder.MOST_LIKED, continuousScrollEnabled = false)

        val changed = controller.request()
        assertEquals(SortOrder.MOST_LIKED, changed.sort)
        assertFalse(changed.continuousScrollEnabled)
        assertEquals(1, changed.page)
        assertTrue(changed.revision > previous.revision)
        assertNull(controller.state.value.totalPages)
    }

    @Test
    fun `manual navigation cannot leave page one before the total is known`() {
        val controller = FeedPagingController(supportsPagination = true)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)
        val initial = controller.state.value

        for (page in listOf(Int.MIN_VALUE, -1, 0, 1, 2, Int.MAX_VALUE)) {
            controller.changePage(page)
            assertEquals(initial, controller.state.value, "Requested page $page")
        }
    }

    @Test
    fun `manual navigation accepts first and last pages but rejects out of range and same page`() {
        val controller = FeedPagingController(supportsPagination = true)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)
        controller.trackedSource().report(pages = 3)
        val first = controller.state.value

        for (page in listOf(-1, 0, 1, 4, Int.MAX_VALUE)) {
            controller.changePage(page)
            assertEquals(first, controller.state.value, "Requested page $page")
        }

        controller.changePage(3)
        val last = controller.state.value
        assertEquals(3, controller.request().page)
        assertEquals(3, last.totalPages)
        assertTrue(controller.request().revision > assertNotNull(first.request).revision)

        for (page in listOf(0, 3, 4)) {
            controller.changePage(page)
            assertEquals(last, controller.state.value, "Requested page $page")
        }

        controller.changePage(1)
        assertEquals(1, controller.request().page)
        assertEquals(3, controller.state.value.totalPages)
        assertTrue(controller.request().revision > assertNotNull(last.request).revision)
    }

    @Test
    fun `continuous scrolling ignores manual navigation even when a total is available`() {
        val controller = FeedPagingController(supportsPagination = true)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = true)
        val source = controller.trackedSource()
        source.report(pages = 8)
        val continuous = controller.state.value

        for (page in listOf(0, 1, 2, 8, 9)) {
            controller.changePage(page)
            assertEquals(continuous, controller.state.value)
        }
        source.report(pages = 3)
        assertEquals(3, controller.state.value.totalPages)
        assertEquals(continuous.request, controller.request())
    }

    @Test
    fun `callbacks from a previous page cannot change totals or roll back the current page`() {
        val controller = FeedPagingController(supportsPagination = true)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)
        val oldSource = controller.trackedSource()
        oldSource.report(pages = 6)
        val pendingOldReport = assertNotNull(oldSource.onPageInfoLoaded)
        controller.changePage(4)
        val selected = controller.state.value

        // 新页的源尚未建立时，旧响应也不能把页码拉回第一页。
        pendingOldReport(1, 1)
        assertEquals(selected, controller.state.value)

        controller.trackedSource().report(pages = 7)
        val loaded = controller.state.value
        pendingOldReport(99, 1980)

        assertEquals(loaded, controller.state.value)
        assertEquals(4, controller.request().page)
        assertEquals(7, controller.state.value.totalPages)
    }

    @Test
    fun `returning to the same sort and page does not revive callbacks from an older request`() {
        val controller = FeedPagingController(supportsPagination = true)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)
        val original = controller.request()
        val oldSource = controller.trackedSource()
        oldSource.report(pages = 8)
        val pendingOldReport = assertNotNull(oldSource.onPageInfoLoaded)

        controller.configure(SortOrder.OLDEST, continuousScrollEnabled = false)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)

        assertEquals(original.sort, controller.request().sort)
        assertEquals(original.page, controller.request().page)
        assertTrue(controller.request().revision > original.revision)
        pendingOldReport(99, 1980)
        assertNull(controller.state.value.totalPages)

        controller.trackedSource().report(pages = 4)
        val loaded = controller.state.value
        pendingOldReport(1, 1)
        assertEquals(loaded, controller.state.value)
    }

    @Test
    fun `replacement source for the same request ignores old callbacks before and after its report`() {
        val controller = FeedPagingController(supportsPagination = true)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)
        val request = controller.request()
        val oldSource = controller.trackedSource()
        oldSource.report(pages = 6)
        val pendingOldReport = assertNotNull(oldSource.onPageInfoLoaded)
        val newSource = controller.trackedSource()
        val awaitingReplacement = controller.state.value

        pendingOldReport(99, 1980)
        assertEquals(awaitingReplacement, controller.state.value)

        newSource.report(pages = 4)
        val loaded = controller.state.value
        pendingOldReport(1, 1)

        assertEquals(loaded, controller.state.value)
        assertEquals(request, controller.request())
        assertEquals(4, controller.state.value.totalPages)
    }

    @Test
    fun `tracking an obsolete request invalidates its source without displacing the active source`() {
        val controller = FeedPagingController(supportsPagination = true)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)
        val obsoleteRequest = controller.request()
        controller.trackedSource().report(pages = 6)
        controller.changePage(2)
        val activeSource = controller.trackedSource()
        activeSource.report(pages = 5)
        val loaded = controller.state.value
        val obsoleteSource = FakePagingSource()

        assertSame(obsoleteSource, controller.track(obsoleteRequest, obsoleteSource))

        assertTrue(obsoleteSource.invalid)
        obsoleteSource.report(pages = 99)
        assertEquals(loaded, controller.state.value)
        activeSource.report(pages = 4)
        assertEquals(4, controller.state.value.totalPages)
        assertEquals(2, controller.request().page)
    }

    @Test
    fun `an invalidated source cannot update metadata even before a replacement is tracked`() {
        val controller = FeedPagingController(supportsPagination = true)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)
        val source = controller.trackedSource()
        source.report(pages = 6)
        val loaded = controller.state.value
        val pendingReport = assertNotNull(source.onPageInfoLoaded)

        source.invalidate()
        pendingReport(1, 1)

        assertEquals(loaded, controller.state.value)
        controller.trackedSource().report(pages = 4)
        assertEquals(4, controller.state.value.totalPages)
    }

    @Test
    fun `shrinking the total rolls back to the new last page and retires the out of range request`() {
        val controller = FeedPagingController(supportsPagination = true)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)
        controller.trackedSource().report(pages = 8)
        controller.changePage(7)
        val outOfRange = controller.request()
        val source = controller.trackedSource()
        val pendingReport = assertNotNull(source.onPageInfoLoaded)

        source.report(pages = 3)

        val rolledBack = controller.state.value
        assertEquals(3, controller.request().page)
        assertEquals(3, rolledBack.totalPages)
        assertTrue(controller.request().revision > outOfRange.revision)
        pendingReport(99, 1980)
        assertEquals(rolledBack, controller.state.value)

        controller.trackedSource().report(pages = 3)
        controller.changePage(2)
        assertEquals(2, controller.request().page)
        assertEquals(3, controller.state.value.totalPages)
    }

    @Test
    fun `zero and negative totals leave a usable first page instead of page zero`() {
        for (reportedPages in listOf(0, -3)) {
            val controller = FeedPagingController(supportsPagination = true)
            controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)
            controller.trackedSource().report(pages = 6)
            controller.changePage(4)
            val previous = controller.request()

            controller.trackedSource().report(pages = reportedPages)

            assertEquals(1, controller.state.value.totalPages)
            assertEquals(1, controller.request().page)
            assertTrue(controller.request().revision > previous.revision)
            val empty = controller.state.value
            controller.changePage(2)
            assertEquals(empty, controller.state.value)
        }
    }

    @Test
    fun `recommendation and random feeds without pagination support always stay continuous`() {
        // 推荐和随机页面向控制器提供 supportsPagination=false。
        val controller = FeedPagingController(supportsPagination = false)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)
        assertTrue(controller.request().continuousScrollEnabled)
        assertEquals(1, controller.request().page)
        controller.trackedSource().report(pages = 8)
        val loaded = controller.state.value

        controller.changePage(2)
        controller.changePage(8)
        assertEquals(loaded, controller.state.value)

        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = true)
        assertEquals(loaded, controller.state.value)
        controller.configure(SortOrder.NEWEST, continuousScrollEnabled = false)
        assertEquals(loaded, controller.state.value)
    }

    @Test
    fun `manual pagination controls respect unknown totals and first middle last and only pages`() {
        val unknown = FeedPagination(continuousScrollEnabled = false)
        assertTrue(unknown.isManual)
        assertFalse(unknown.hasPrevious)
        assertFalse(unknown.hasNext)

        val first = unknown.copy(totalPages = 3)
        assertFalse(first.hasPrevious)
        assertTrue(first.hasNext)

        val middle = first.copy(currentPage = 2)
        assertTrue(middle.hasPrevious)
        assertTrue(middle.hasNext)

        val last = first.copy(currentPage = 3)
        assertTrue(last.hasPrevious)
        assertFalse(last.hasNext)

        val only = unknown.copy(totalPages = 1)
        assertFalse(only.hasPrevious)
        assertFalse(only.hasNext)
    }

    @Test
    fun `continuous and unsupported feeds never expose manual pagination controls`() {
        val paginations = listOf(
            FeedPagination(continuousScrollEnabled = true, currentPage = 2, totalPages = 3),
            FeedPagination(
                continuousScrollEnabled = false,
                currentPage = 2,
                totalPages = 3,
                supportsPagination = false,
            ),
        )

        for (pagination in paginations) {
            assertFalse(pagination.isManual)
            assertFalse(pagination.hasPrevious)
            assertFalse(pagination.hasNext)
        }
    }

    private fun FeedPagingController.request(): FeedPagingRequest = assertNotNull(state.value.request)

    private fun FeedPagingController.trackedSource() = FakePagingSource().also {
        track(request(), it)
    }

    private class FakePagingSource : PagingSource<Int, String>(), PageInfoReporting {
        override var onPageInfoLoaded: ((totalPages: Int, totalCount: Int) -> Unit)? = null

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> =
            error("Controller tests only deliver page metadata")

        override fun getRefreshKey(state: PagingState<Int, String>): Int? = null

        fun report(pages: Int, total: Int = 0) {
            onPageInfoLoaded?.invoke(pages, total)
        }
    }
}
