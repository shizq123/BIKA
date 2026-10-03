package com.shizq.bika.ui.feed

/**
 * Feed 顶部摘要的结构化展示状态。
 *
 * UI 层只负责把该状态映射到字符串资源，避免在 Composable 中重复推导业务状态。
 */
internal sealed interface FeedSummaryUiState {
    data object Loading : FeedSummaryUiState
    data object LoadFailed : FeedSummaryUiState
    data object Refreshing : FeedSummaryUiState
    data class LoadingPage(val page: Int) : FeedSummaryUiState
    data class VisibleOnPage(
        val page: Int,
        val totalPages: Int,
        val visibleCount: Int,
    ) : FeedSummaryUiState

    data class TotalCount(val count: Int) : FeedSummaryUiState
    data class PageIndicator(
        val page: Int,
        val totalPages: Int,
    ) : FeedSummaryUiState
}

internal fun FeedContentState.toSummaryUiState(): FeedSummaryUiState = when (this) {
    FeedContentState.Initial,
    FeedContentState.Loading -> FeedSummaryUiState.Loading

    is FeedContentState.Error -> FeedSummaryUiState.LoadFailed
    is FeedContentState.Success -> {
        val loading = refreshState as? FeedRefreshState.Loading
        when {
            loading != null && loading.targetQuery.page != page.page ->
                FeedSummaryUiState.LoadingPage(loading.targetQuery.page)

            loading != null -> FeedSummaryUiState.Refreshing
            isClientFiltered -> FeedSummaryUiState.VisibleOnPage(
                page = page.page,
                totalPages = page.totalPages,
                visibleCount = page.items.size,
            )

            page.totalCount != null -> FeedSummaryUiState.TotalCount(page.totalCount)
            else -> FeedSummaryUiState.PageIndicator(
                page = page.page,
                totalPages = page.totalPages,
            )
        }
    }
}

internal data class FeedPaginationUiState(
    val currentPage: Int,
    val totalPages: Int,
    val previousEnabled: Boolean,
    val nextEnabled: Boolean,
    val indicatorEnabled: Boolean,
)

internal fun FeedContentState.toPaginationUiState(): FeedPaginationUiState? {
    val success = this as? FeedContentState.Success ?: return null
    val page = success.page
    if (page.totalPages <= 1) return null

    val enabled = success.refreshState !is FeedRefreshState.Loading
    val currentPage = page.page.coerceIn(1, page.totalPages)
    return FeedPaginationUiState(
        currentPage = currentPage,
        totalPages = page.totalPages,
        previousEnabled = enabled && currentPage > 1,
        nextEnabled = enabled && currentPage < page.totalPages,
        indicatorEnabled = enabled,
    )
}
