package com.shizq.bika.ui.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedPresentationTest {

    @Test
    fun `初始加载和初始错误映射为对应摘要`() {
        assertEquals(FeedSummaryUiState.Loading, FeedContentState.Initial.toSummaryUiState())
        assertEquals(FeedSummaryUiState.Loading, FeedContentState.Loading.toSummaryUiState())
        assertEquals(
            FeedSummaryUiState.LoadFailed,
            FeedContentState.Error(FeedError.LoadFailed).toSummaryUiState(),
        )
    }

    @Test
    fun `跨页请求优先显示目标页`() {
        val content = success(
            currentPage = 1,
            refreshState = FeedRefreshState.Loading(FeedQuery(page = 3)),
        )

        assertEquals(
            FeedSummaryUiState.LoadingPage(page = 3),
            content.toSummaryUiState(),
        )
    }

    @Test
    fun `同页刷新显示刷新状态`() {
        val content = success(
            currentPage = 2,
            refreshState = FeedRefreshState.Loading(FeedQuery(page = 2)),
        )

        assertEquals(FeedSummaryUiState.Refreshing, content.toSummaryUiState())
    }

    @Test
    fun `客户端筛选显示本页可见数量`() {
        val content = success(currentPage = 2, isClientFiltered = true)

        assertEquals(
            FeedSummaryUiState.VisibleOnPage(page = 2, totalPages = 3, visibleCount = 0),
            content.toSummaryUiState(),
        )
    }

    @Test
    fun `服务端总数存在时显示总数否则显示页码`() {
        assertEquals(
            FeedSummaryUiState.TotalCount(20),
            success(totalCount = 20).toSummaryUiState(),
        )
        assertEquals(
            FeedSummaryUiState.PageIndicator(page = 1, totalPages = 3),
            success(totalCount = null).toSummaryUiState(),
        )
    }

    @Test
    fun `非成功或单页内容不提供分页状态`() {
        assertNull(FeedContentState.Loading.toPaginationUiState())
        assertNull(success(totalPages = 1).toPaginationUiState())
    }

    @Test
    fun `第一页和最后一页正确禁用边界按钮`() {
        val first = success(currentPage = 1).toPaginationUiState()!!
        assertFalse(first.previousEnabled)
        assertTrue(first.nextEnabled)

        val last = success(currentPage = 3).toPaginationUiState()!!
        assertTrue(last.previousEnabled)
        assertFalse(last.nextEnabled)
    }

    @Test
    fun `异常当前页会被分页模型约束到有效范围`() {
        assertEquals(1, success(currentPage = 0).toPaginationUiState()!!.currentPage)
        assertEquals(3, success(currentPage = 99).toPaginationUiState()!!.currentPage)
    }

    @Test
    fun `刷新期间禁用全部分页交互`() {
        val pagination = success(
            currentPage = 2,
            refreshState = FeedRefreshState.Loading(FeedQuery(page = 3)),
        ).toPaginationUiState()!!

        assertFalse(pagination.previousEnabled)
        assertFalse(pagination.nextEnabled)
        assertFalse(pagination.indicatorEnabled)
    }

    private fun success(
        currentPage: Int = 1,
        totalPages: Int = 3,
        totalCount: Int? = null,
        isClientFiltered: Boolean = false,
        refreshState: FeedRefreshState = FeedRefreshState.Idle,
    ) = FeedContentState.Success(
        page = FeedPage(
            items = emptyList(),
            page = currentPage,
            totalPages = totalPages,
            totalCount = totalCount,
        ),
        displayedQuery = FeedQuery(page = currentPage),
        isClientFiltered = isClientFiltered,
        refreshState = refreshState,
    )
}
