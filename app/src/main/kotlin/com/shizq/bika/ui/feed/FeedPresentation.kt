package com.shizq.bika.ui.feed

/** Feed 顶部摘要的结构化展示状态。 */
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
    data class PageIndicator(val page: Int, val totalPages: Int) : FeedSummaryUiState
}

internal fun FeedUiState.toSummaryUiState(): FeedSummaryUiState = when (this) {
    is FeedUiState.Initial -> FeedSummaryUiState.Loading
    is FeedUiState.Error -> FeedSummaryUiState.LoadFailed
    is FeedUiState.Content -> {
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
            else -> FeedSummaryUiState.PageIndicator(page.page, page.totalPages)
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

internal fun FeedUiState.toPaginationUiState(): FeedPaginationUiState? {
    val content = this as? FeedUiState.Content ?: return null
    if (content.page.totalPages <= 1) return null

    val enabled = content.refreshState !is FeedRefreshState.Loading
    val currentPage = content.page.page.coerceIn(1, content.page.totalPages)
    return FeedPaginationUiState(
        currentPage = currentPage,
        totalPages = content.page.totalPages,
        previousEnabled = enabled && currentPage > 1,
        nextEnabled = enabled && currentPage < content.page.totalPages,
        indicatorEnabled = enabled,
    )
}
