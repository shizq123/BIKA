package com.shizq.bika.ui.feed

import com.shizq.bika.core.model.SortOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedStateTest {

    @Test
    fun `内容状态同时保留展示查询和目标查询`() {
        val displayedQuery = FeedQuery(page = 1, sort = SortOrder.NEWEST)
        val targetQuery = FeedQuery(page = 2, sort = SortOrder.NEWEST)
        val state = FeedUiState.Content(
            data = FeedData(query = targetQuery),
            page = FeedPage(
                items = emptyList(),
                page = 1,
                totalPages = 10,
                totalCount = 100,
            ),
            displayedQuery = displayedQuery,
            refreshState = FeedRefreshState.Loading(targetQuery),
        )

        assertEquals(1, state.page.page)
        assertEquals(displayedQuery, state.displayedQuery)
        assertEquals(targetQuery, state.data.query)
        assertEquals(targetQuery, (state.refreshState as FeedRefreshState.Loading).targetQuery)
    }

    @Test
    fun `刷新失败保留失败目标和错误原因`() {
        val targetQuery = FeedQuery(page = 3)
        val failed = FeedRefreshState.Failed(
            targetQuery = targetQuery,
            error = FeedError.LoadFailed,
        )

        assertEquals(targetQuery, failed.targetQuery)
        assertEquals(FeedError.LoadFailed, failed.error)
    }

    @Test
    fun `追加状态正确表达且可感知连续滚动开关配置`() {
        val state = FeedUiState.Content(
            data = FeedData(continuousScrollEnabled = true),
            page = FeedPage(
                items = emptyList(),
                page = 1,
                totalPages = 5,
                totalCount = 50,
            ),
            displayedQuery = FeedQuery(page = 1),
            refreshState = FeedRefreshState.Appending,
        )

        assertEquals(true, state.continuousScrollEnabled)
        assertEquals(FeedRefreshState.Appending, state.refreshState)
    }
}
