package com.shizq.bika.ui.feed

import com.shizq.bika.core.model.SortOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FeedStateTest {

    @Test
    fun `刷新状态同时保留展示查询和目标查询`() {
        val displayedQuery = FeedQuery(page = 1, sort = SortOrder.NEWEST)
        val targetQuery = FeedQuery(page = 2, sort = SortOrder.NEWEST)
        val content = FeedContentState.Success(
            page = FeedPage(
                items = emptyList(),
                page = 1,
                totalPages = 10,
                totalCount = 100,
            ),
            displayedQuery = displayedQuery,
            refreshState = FeedRefreshState.Loading(targetQuery),
        )

        assertEquals(1, content.page.page)
        assertEquals(displayedQuery, content.displayedQuery)
        assertEquals(
            targetQuery,
            assertIs<FeedRefreshState.Loading>(content.refreshState).targetQuery
        )
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
}
