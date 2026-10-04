package com.shizq.bika.ui.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedPresentationTest {

    @Test
    fun `初始加载和初始错误映射为对应摘要`() {
        assertEquals(FeedSummaryUiState.Loading, FeedUiState.Initial().toSummaryUiState())
        assertEquals(
            FeedSummaryUiState.LoadFailed,
            FeedUiState.Error(FeedData(), FeedError.LoadFailed).toSummaryUiState(),
        )
    }

    @Test
    fun `跨页请求优先显示目标页`() {
        val state = content(
            currentPage = 1,
            refreshState = FeedRefreshState.Loading(FeedQuery(page = 3)),
        )
        assertEquals(FeedSummaryUiState.LoadingPage(3), state.toSummaryUiState())
    }

    @Test
    fun `同页刷新显示刷新状态`() {
        val state = content(
            currentPage = 2,
            refreshState = FeedRefreshState.Loading(FeedQuery(page = 2)),
        )
        assertEquals(FeedSummaryUiState.Refreshing, state.toSummaryUiState())
    }

    @Test
    fun `客户端筛选显示本页可见数量`() {
        assertEquals(
            FeedSummaryUiState.VisibleOnPage(2, 3, 0),
            content(currentPage = 2, isClientFiltered = true).toSummaryUiState(),
        )
    }

    @Test
    fun `服务端总数存在时显示总数否则显示页码`() {
        assertEquals(FeedSummaryUiState.TotalCount(20), content(totalCount = 20).toSummaryUiState())
        assertEquals(
            FeedSummaryUiState.PageIndicator(1, 3),
            content(totalCount = null).toSummaryUiState(),
        )
    }

    @Test
    fun `非内容或单页状态不提供分页`() {
        assertNull(FeedUiState.Initial().toPaginationUiState())
        assertNull(content(totalPages = 1).toPaginationUiState())
    }

    @Test
    fun `第一页和最后一页正确禁用边界按钮`() {
        val first = content(currentPage = 1).toPaginationUiState()!!
        assertFalse(first.previousEnabled)
        assertTrue(first.nextEnabled)

        val last = content(currentPage = 3).toPaginationUiState()!!
        assertTrue(last.previousEnabled)
        assertFalse(last.nextEnabled)
    }

    @Test
    fun `异常当前页会被分页模型约束到有效范围`() {
        assertEquals(1, content(currentPage = 0).toPaginationUiState()!!.currentPage)
        assertEquals(3, content(currentPage = 99).toPaginationUiState()!!.currentPage)
    }

    @Test
    fun `刷新期间禁用全部分页交互`() {
        val pagination = content(
            currentPage = 2,
            refreshState = FeedRefreshState.Loading(FeedQuery(page = 3)),
        ).toPaginationUiState()!!
        assertFalse(pagination.previousEnabled)
        assertFalse(pagination.nextEnabled)
        assertFalse(pagination.indicatorEnabled)
    }

    private fun content(
        currentPage: Int = 1,
        totalPages: Int = 3,
        totalCount: Int? = null,
        isClientFiltered: Boolean = false,
        refreshState: FeedRefreshState = FeedRefreshState.Idle,
    ) = FeedUiState.Content(
        data = FeedData(query = FeedQuery(page = currentPage)),
        page = FeedPage(emptyList(), currentPage, totalPages, totalCount),
        displayedQuery = FeedQuery(page = currentPage),
        isClientFiltered = isClientFiltered,
        refreshState = refreshState,
    )
}
