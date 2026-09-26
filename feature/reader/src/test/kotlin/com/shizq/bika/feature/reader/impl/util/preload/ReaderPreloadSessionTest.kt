package com.shizq.bika.feature.reader.impl.util.preload

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil3.request.ImageRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ReaderPreloadSessionTest {

    @Test
    fun `rapid viewport updates are conflated to the latest window`() = runTest {
        val requested = mutableListOf<Int>()
        val session = ReaderPreloadSession(
            scope = backgroundScope,
            dataProvider = FakeDataProvider(100),
            modelProvider = RecordingModelProvider(requested),
            enqueuer = RecordingEnqueuer(),
            closeEnqueuer = {},
        )

        session.submitViewport(
            ViewportSnapshot(visibleRange = 0..4),
            preloadCount = 2,
        )
        session.submitViewport(
            ViewportSnapshot(visibleRange = 10..14),
            preloadCount = 3,
        )
        runCurrent()

        assertEquals(listOf(15, 16, 17), requested)
        session.close()
    }

    @Test
    fun `null viewport clears the active window through the session`() = runTest {
        val enqueuer = RecordingEnqueuer()
        val session = ReaderPreloadSession(
            scope = backgroundScope,
            dataProvider = FakeDataProvider(100),
            modelProvider = RecordingModelProvider(mutableListOf()),
            enqueuer = enqueuer,
            closeEnqueuer = {},
        )

        session.submitViewport(
            ViewportSnapshot(visibleRange = null),
            preloadCount = 3,
        )
        runCurrent()

        assertEquals(listOf(emptyList()), enqueuer.windows)
        session.close()
    }

    @Test
    fun `a newer generation resets the old session window`() = runTest {
        val requested = mutableListOf<Int>()
        val session = ReaderPreloadSession(
            scope = backgroundScope,
            dataProvider = FakeDataProvider(100),
            modelProvider = RecordingModelProvider(requested),
            enqueuer = RecordingEnqueuer(),
            closeEnqueuer = {},
        )

        session.submitViewport(
            ViewportSnapshot(
                visibleRange = 40..44,
                direction = ScrollDirection.Backward,
                cause = ViewportChangeCause.UserScroll,
                generation = 0,
            ),
            preloadCount = 2,
        )
        runCurrent()
        requested.clear()

        session.submitViewport(
            ViewportSnapshot(
                visibleRange = 0..4,
                cause = ViewportChangeCause.DataRefresh,
                generation = 1,
            ),
            preloadCount = 2,
        )
        runCurrent()

        assertEquals(listOf(5, 6), requested)
        session.close()
    }

    @Test
    fun `placeholder items are skipped without stopping the planned window`() = runTest {
        val requested = mutableListOf<Int>()
        val session = ReaderPreloadSession(
            scope = backgroundScope,
            dataProvider = FakeDataProvider(100, unavailable = setOf(6, 7)),
            modelProvider = RecordingModelProvider(requested),
            enqueuer = RecordingEnqueuer(),
            closeEnqueuer = {},
        )

        session.submitViewport(
            ViewportSnapshot(
                visibleRange = 0..4,
                direction = ScrollDirection.Forward,
                cause = ViewportChangeCause.UserScroll,
            ),
            preloadCount = 4,
        )
        runCurrent()

        assertEquals(listOf(5, 8), requested)
        session.close()
    }

    @Test
    fun `zero preload count clears the active window`() = runTest {
        val enqueuer = RecordingEnqueuer()
        val session = ReaderPreloadSession(
            scope = backgroundScope,
            dataProvider = FakeDataProvider(100),
            modelProvider = RecordingModelProvider(mutableListOf()),
            enqueuer = enqueuer,
            closeEnqueuer = {},
        )

        session.submitViewport(ViewportSnapshot(visibleRange = 0..4), preloadCount = 0)
        runCurrent()

        assertEquals(listOf(emptyList()), enqueuer.windows)
        session.close()
    }

    @Test
    fun `older generation is ignored after a newer window`() = runTest {
        val requested = mutableListOf<Int>()
        val session = ReaderPreloadSession(
            scope = backgroundScope,
            dataProvider = FakeDataProvider(100),
            modelProvider = RecordingModelProvider(requested),
            enqueuer = RecordingEnqueuer(),
            closeEnqueuer = {},
        )

        session.submitViewport(
            ViewportSnapshot(visibleRange = 20..24, generation = 2),
            preloadCount = 2,
        )
        runCurrent()
        requested.clear()

        session.submitViewport(
            ViewportSnapshot(visibleRange = 0..4, generation = 1),
            preloadCount = 2,
        )
        runCurrent()

        assertTrue(requested.isEmpty())
        session.close()
    }

    @Test
    fun `older generation is rejected before conflation can replace a newer event`() = runTest {
        val requested = mutableListOf<Int>()
        val session = ReaderPreloadSession(
            scope = backgroundScope,
            dataProvider = FakeDataProvider(100),
            modelProvider = RecordingModelProvider(requested),
            enqueuer = RecordingEnqueuer(),
            closeEnqueuer = {},
        )

        session.submitViewport(
            ViewportSnapshot(visibleRange = 20..24, generation = 2),
            preloadCount = 2,
        )
        session.submitViewport(
            ViewportSnapshot(visibleRange = 0..4, generation = 1),
            preloadCount = 2,
        )
        runCurrent()

        assertEquals(listOf(25, 26), requested)
        session.close()
    }

    @Test
    fun `same preload key is emitted only once`() = runTest {
        val enqueuer = RecordingEnqueuer()
        val session = ReaderPreloadSession(
            scope = backgroundScope,
            dataProvider = FakeDataProvider(100),
            modelProvider = SameKeyModelProvider,
            enqueuer = enqueuer,
            closeEnqueuer = {},
        )

        session.submitViewport(
            ViewportSnapshot(
                visibleRange = 0..1,
                direction = ScrollDirection.Forward,
                cause = ViewportChangeCause.UserScroll,
            ),
            preloadCount = 3,
        )
        runCurrent()

        assertEquals(1, enqueuer.windows.single().size)
        assertEquals("same-page", enqueuer.windows.single().single().key)
        session.close()
    }

    @Test
    fun `visible requests are forwarded separately from the next preload window`() = runTest {
        val enqueuer = RecordingEnqueuer()
        val session = ReaderPreloadSession(
            scope = backgroundScope,
            dataProvider = FakeDataProvider(100),
            modelProvider = RecordingRequestModelProvider,
            enqueuer = enqueuer,
            closeEnqueuer = {},
        )

        session.submitViewport(
            ViewportSnapshot(
                visibleRange = 0..0,
                direction = ScrollDirection.Forward,
                cause = ViewportChangeCause.UserScroll,
            ),
            preloadCount = 1,
        )
        runCurrent()

        session.submitViewport(
            ViewportSnapshot(
                visibleRange = 1..1,
                direction = ScrollDirection.Forward,
                cause = ViewportChangeCause.UserScroll,
            ),
            preloadCount = 1,
        )
        runCurrent()

        assertEquals(listOf(1), enqueuer.windows[0].map(PreloadRequest::index))
        assertEquals(listOf(2), enqueuer.windows[1].map(PreloadRequest::index))
        assertEquals(listOf(1), enqueuer.visibleWindows[1].map(PreloadRequest::index))
        session.close()
    }

    @Test
    fun `closing the session releases the executor and ignores later input`() = runTest {
        var closed = false
        val requested = mutableListOf<Int>()
        val session = ReaderPreloadSession(
            scope = backgroundScope,
            dataProvider = FakeDataProvider(100),
            modelProvider = RecordingModelProvider(requested),
            enqueuer = RecordingEnqueuer(),
            closeEnqueuer = { closed = true },
        )

        session.close()
        session.submitViewport(
            ViewportSnapshot(visibleRange = 0..4),
            preloadCount = 3,
        )
        runCurrent()

        assertTrue(closed)
        assertTrue(requested.isEmpty())
    }

    private class FakeDataProvider(
        override val itemCount: Int,
        private val unavailable: Set<Int> = emptySet(),
    ) : PreloadDataProvider<Int> {
        override fun getItem(index: Int): Int? =
            if (index in unavailable) null else index
    }

    private class RecordingModelProvider(
        private val requested: MutableList<Int>,
    ) : PreloadModelProvider<Int> {
        override fun getPreloadRequest(item: Int): ImageRequest? {
            requested += item
            return null
        }
    }

    private object SameKeyModelProvider : PreloadModelProvider<Int> {
        private val context: Context
            get() = ApplicationProvider.getApplicationContext()

        override fun getPreloadRequest(item: Int): ImageRequest =
            ImageRequest.Builder(context)
                .data(item)
                .build()

        override fun getPreloadKey(item: Int, request: ImageRequest): String =
            "same-page"
    }

    private object RecordingRequestModelProvider : PreloadModelProvider<Int> {
        private val context: Context
            get() = ApplicationProvider.getApplicationContext()

        override fun getPreloadRequest(item: Int): ImageRequest =
            ImageRequest.Builder(context)
                .data(item)
                .build()

        override fun getPreloadKey(item: Int, request: ImageRequest): String =
            "page-$item"
    }

    private class RecordingEnqueuer : PreloadRequestEnqueuer {
        val windows = mutableListOf<List<PreloadRequest>>()
        val visibleWindows = mutableListOf<List<PreloadRequest>>()

        override fun updateWindow(
            requests: List<PreloadRequest>,
            visibleRequests: List<PreloadRequest>,
        ) {
            windows += requests
            visibleWindows += visibleRequests
        }
    }
}
