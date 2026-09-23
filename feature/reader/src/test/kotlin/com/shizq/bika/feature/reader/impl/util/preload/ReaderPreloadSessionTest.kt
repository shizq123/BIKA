package com.shizq.bika.feature.reader.impl.util.preload

import coil3.request.ImageRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

        session.submitViewport(0..4, preloadCount = 2)
        session.submitViewport(10..14, preloadCount = 3)
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

        session.submitViewport(null, preloadCount = 3)
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
        session.submitViewport(0..4, preloadCount = 3)
        runCurrent()

        assertTrue(closed)
        assertTrue(requested.isEmpty())
    }

    private class FakeDataProvider(
        override val itemCount: Int,
    ) : PreloadDataProvider<Int> {
        override fun getItem(index: Int): Int = index
    }

    private class RecordingModelProvider(
        private val requested: MutableList<Int>,
    ) : PreloadModelProvider<Int> {
        override fun getPreloadRequest(item: Int): ImageRequest? {
            requested += item
            return null
        }
    }

    private class RecordingEnqueuer : PreloadRequestEnqueuer {
        val windows = mutableListOf<List<PreloadRequest>>()

        override fun updateWindow(
            requests: List<PreloadRequest>,
            visibleRequests: List<PreloadRequest>,
        ) {
            windows += requests
        }
    }
}
