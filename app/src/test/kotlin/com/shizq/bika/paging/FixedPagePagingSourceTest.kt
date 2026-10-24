package com.shizq.bika.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FixedPagePagingSourceTest {
    @Test
    fun `refresh append and prepend always load the selected page without adjacent keys`() = runTest {
        val data = listOf("second", "first", "second")
        val source = FakePagingSource {
            LoadResult.Page(data = data, prevKey = 4, nextKey = 6)
        }
        val fixed = FixedPagePagingSource(source, page = 5)
        val requests = listOf(
            refresh(),
            refresh(key = 99, loadSize = 60),
            LoadParams.Append(key = 6, loadSize = 40, placeholdersEnabled = true),
            LoadParams.Prepend(key = 4, loadSize = 20, placeholdersEnabled = true),
        )

        for (params in requests) {
            val result = assertIs<LoadResult.Page<Int, String>>(fixed.load(params))
            val delegated = assertIs<LoadParams.Refresh<Int>>(source.requests.last())

            assertEquals(5, delegated.key)
            assertEquals(params.loadSize, delegated.loadSize)
            assertFalse(delegated.placeholdersEnabled)
            assertEquals(data, result.data)
            assertNull(result.prevKey)
            assertNull(result.nextKey)
        }
        assertEquals(requests.size, source.requests.size)
    }

    @Test
    fun `refresh key stays on the selected last page even without adjacent keys or an anchor`() = runTest {
        val source = FakePagingSource {
            LoadResult.Page(data = listOf("last comic"), prevKey = 5, nextKey = null)
        }
        val fixed = FixedPagePagingSource(source, page = 6)
        val lastPage = assertIs<LoadResult.Page<Int, String>>(fixed.load(refresh()))
        val config = PagingConfig(pageSize = 20, enablePlaceholders = false)

        for (anchor in listOf(null, 0)) {
            val state = PagingState(
                pages = listOf(lastPage),
                anchorPosition = anchor,
                config = config,
                leadingPlaceholderCount = 0,
            )
            assertEquals(6, fixed.getRefreshKey(state))
        }
        assertEquals(
            6,
            fixed.getRefreshKey(
                PagingState(
                    pages = emptyList(),
                    anchorPosition = null,
                    config = config,
                    leadingPlaceholderCount = 0,
                ),
            ),
        )
    }

    @Test
    fun `page metadata is bridged unchanged instead of inferred from returned items`() = runTest {
        for ((pages, total) in listOf(9 to 173, 0 to 0)) {
            val source = FakePagingSource {
                report(pages, total)
                LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
            }
            val fixed = FixedPagePagingSource(source, page = 1)
            val reports = mutableListOf<Pair<Int, Int>>()
            fixed.onPageInfoLoaded = { reportedPages, reportedTotal ->
                reports += reportedPages to reportedTotal
            }

            val result = assertIs<LoadResult.Page<Int, String>>(fixed.load(refresh()))

            assertTrue(result.data.isEmpty())
            assertEquals(listOf(pages to total), reports)
        }
    }

    @Test
    fun `load error preserves the cause and retry uses the same selected page`() = runTest {
        val failure = IOException("Temporary failure")
        var shouldFail = true
        val source = FakePagingSource {
            if (shouldFail) {
                LoadResult.Error(failure)
            } else {
                report(pages = 8, total = 153)
                LoadResult.Page(data = listOf("recovered"), prevKey = 2, nextKey = 4)
            }
        }
        val fixed = FixedPagePagingSource(source, page = 3)
        val reports = mutableListOf<Pair<Int, Int>>()
        fixed.onPageInfoLoaded = { pages, total -> reports += pages to total }

        val error = assertIs<LoadResult.Error<Int, String>>(fixed.load(refresh(key = 1)))

        assertSame(failure, error.throwable)
        assertFalse(fixed.invalid)
        assertFalse(source.invalid)
        assertTrue(reports.isEmpty())

        shouldFail = false
        val retry = assertIs<LoadResult.Page<Int, String>>(fixed.load(refresh(key = 99)))

        assertEquals(listOf("recovered"), retry.data)
        assertNull(retry.prevKey)
        assertNull(retry.nextKey)
        assertEquals(listOf(3, 3), source.requests.map { it.key })
        assertEquals(listOf(8 to 153), reports)
    }

    @Test
    fun `cancellation is rethrown without becoming an error or reporting page metadata`() = runTest {
        val cancellation = CancellationException("Load cancelled")
        val source = FakePagingSource { throw cancellation }
        val fixed = FixedPagePagingSource(source, page = 4)
        val reports = mutableListOf<Pair<Int, Int>>()
        fixed.onPageInfoLoaded = { pages, total -> reports += pages to total }

        val thrown = assertFailsWith<CancellationException> { fixed.load(refresh()) }

        assertSame(cancellation, thrown)
        assertTrue(reports.isEmpty())
        assertEquals(listOf(4), source.requests.map { it.key })
    }

    @Test
    fun `invalid load result stays invalid instead of becoming an empty successful page`() = runTest {
        val source = FakePagingSource { LoadResult.Invalid() }
        val fixed = FixedPagePagingSource(source, page = 2)
        val reports = mutableListOf<Pair<Int, Int>>()
        fixed.onPageInfoLoaded = { pages, total -> reports += pages to total }

        assertIs<LoadResult.Invalid<Int, String>>(fixed.load(refresh()))
        assertEquals(listOf(2), source.requests.map { it.key })
        assertTrue(reports.isEmpty())
    }

    @Test
    fun `invalidating the wrapper invalidates the source and prevents further loads`() = runTest {
        val source = FakePagingSource()
        val fixed = FixedPagePagingSource(source, page = 2)
        var sourceInvalidations = 0
        var wrapperInvalidations = 0
        source.registerInvalidatedCallback { sourceInvalidations++ }
        fixed.registerInvalidatedCallback { wrapperInvalidations++ }

        fixed.invalidate()
        fixed.invalidate()
        source.invalidate()

        assertTrue(fixed.invalid)
        assertTrue(source.invalid)
        assertEquals(1, sourceInvalidations)
        assertEquals(1, wrapperInvalidations)
        assertIs<LoadResult.Invalid<Int, String>>(fixed.load(refresh()))
        assertTrue(source.requests.isEmpty())
    }

    @Test
    fun `invalidating the source invalidates the wrapper and prevents further loads`() = runTest {
        val source = FakePagingSource()
        val fixed = FixedPagePagingSource(source, page = 2)
        var wrapperInvalidations = 0
        fixed.registerInvalidatedCallback { wrapperInvalidations++ }

        source.invalidate()
        source.invalidate()

        assertTrue(fixed.invalid)
        assertEquals(1, wrapperInvalidations)
        assertIs<LoadResult.Invalid<Int, String>>(fixed.load(refresh()))
        assertTrue(source.requests.isEmpty())
    }

    @Test
    fun `wrapping an already invalid source immediately produces an invalid wrapper`() = runTest {
        val source = FakePagingSource().also { it.invalidate() }
        val fixed = FixedPagePagingSource(source, page = 1)

        assertTrue(fixed.invalid)
        assertIs<LoadResult.Invalid<Int, String>>(fixed.load(refresh()))
        assertTrue(source.requests.isEmpty())
    }

    @Test
    fun `a page completing after invalidation is discarded along with its late metadata`() = runTest {
        val started = CompletableDeferred<Unit>()
        val complete = CompletableDeferred<Unit>()
        val source = FakePagingSource {
            // 保留已在途的回调，失效并不保证底层加载会及时停止。
            val pendingReport = assertNotNull(onPageInfoLoaded)
            started.complete(Unit)
            complete.await()
            pendingReport(99, 1980)
            LoadResult.Page(data = listOf("stale"), prevKey = null, nextKey = 2)
        }
        val fixed = FixedPagePagingSource(source, page = 1)
        val reports = mutableListOf<Pair<Int, Int>>()
        fixed.onPageInfoLoaded = { pages, total -> reports += pages to total }
        val loading = async { fixed.load(refresh()) }

        started.await()
        fixed.invalidate()
        complete.complete(Unit)

        assertIs<LoadResult.Invalid<Int, String>>(loading.await())
        assertTrue(reports.isEmpty())
        assertEquals(1, source.requests.size)
    }

    @Test
    fun `old source callbacks cannot overwrite metadata from a replacement wrapper`() {
        val oldSource = FakePagingSource()
        val oldWrapper = FixedPagePagingSource(oldSource, page = 1)
        val reports = mutableListOf<Pair<Int, Int>>()
        oldWrapper.onPageInfoLoaded = { pages, total -> reports += pages to total }
        val pendingOldReport = assertNotNull(oldSource.onPageInfoLoaded)
        oldWrapper.invalidate()

        val newSource = FakePagingSource()
        val newWrapper = FixedPagePagingSource(newSource, page = 2)
        newWrapper.onPageInfoLoaded = { pages, total -> reports += pages to total }

        pendingOldReport(99, 1980)
        assertTrue(reports.isEmpty())
        newSource.report(pages = 4, total = 63)
        pendingOldReport(1, 1)

        assertEquals(listOf(4 to 63), reports)
    }

    private fun refresh(key: Int? = null, loadSize: Int = 20) = LoadParams.Refresh(
        key = key,
        loadSize = loadSize,
        placeholdersEnabled = true,
    )

    private class FakePagingSource(
        private val loadBlock: suspend FakePagingSource.(LoadParams<Int>) -> LoadResult<Int, String> = {
            LoadResult.Page(data = listOf("comic"), prevKey = null, nextKey = null)
        },
    ) : PagingSource<Int, String>(), PageInfoReporting {
        override var onPageInfoLoaded: ((totalPages: Int, totalCount: Int) -> Unit)? = null
        val requests = mutableListOf<LoadParams<Int>>()

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
            requests += params
            return loadBlock(this, params)
        }

        override fun getRefreshKey(state: PagingState<Int, String>): Int? = null

        fun report(pages: Int, total: Int) {
            onPageInfoLoaded?.invoke(pages, total)
        }
    }
}
