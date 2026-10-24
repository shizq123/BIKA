package com.shizq.bika.ui.feed

import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.SortOrder
import com.shizq.bika.paging.PageInfoReporting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class FeedPagingRequest(
    val sort: SortOrder,
    val continuousScrollEnabled: Boolean,
    val page: Int = 1,
    val revision: Long,
)

internal data class FeedPagingSession(
    val request: FeedPagingRequest,
    val comics: Flow<PagingData<ComicSummary>>,
)

internal data class FeedPagingState(
    val request: FeedPagingRequest? = null,
    val totalPages: Int? = null,
)

/** 页码切换与元数据回调可能来自不同线程，旧请求不能回写新目录的页数。 */
internal class FeedPagingController(private val supportsPagination: Boolean) {
    private val mutableState = MutableStateFlow(FeedPagingState())
    val state = mutableState.asStateFlow()
    private var revision = 0L
    private var sourceGeneration = 0L

    @Synchronized
    fun configure(sort: SortOrder, continuousScrollEnabled: Boolean) {
        val continuous = continuousScrollEnabled || !supportsPagination
        val current = mutableState.value.request
        if (current?.sort == sort && current.continuousScrollEnabled == continuous) return
        mutableState.value = FeedPagingState(
            request = FeedPagingRequest(sort, continuous, revision = ++revision),
        )
    }

    @Synchronized
    fun changePage(page: Int) {
        val current = mutableState.value
        val request = current.request ?: return
        if (request.continuousScrollEnabled || page == request.page) return
        if (page < 1 || page > (current.totalPages ?: request.page)) return
        mutableState.value = current.copy(request = request.copy(page = page, revision = ++revision))
    }

    @Synchronized
    fun <T : Any> track(
        request: FeedPagingRequest,
        source: PagingSource<Int, T>,
    ): PagingSource<Int, T> {
        require(source is PageInfoReporting)
        if (mutableState.value.request != request) {
            source.invalidate()
            return source
        }
        val generation = ++sourceGeneration
        source.onPageInfoLoaded = { pages, _ ->
            synchronized(this) {
                if (!source.invalid && generation == sourceGeneration && mutableState.value.request == request) {
                    val totalPages = pages.coerceAtLeast(1)
                    val current = mutableState.value
                    mutableState.value = current.copy(
                        totalPages = totalPages,
                        request = if (!request.continuousScrollEnabled && request.page > totalPages) {
                            request.copy(page = totalPages, revision = ++revision)
                        } else {
                            request
                        },
                    )
                }
            }
        }
        return source
    }
}
