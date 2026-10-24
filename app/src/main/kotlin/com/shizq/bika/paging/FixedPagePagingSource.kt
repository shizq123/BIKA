package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState

/** 手动翻页只向 Paging 暴露指定接口页，访问末项也不会触发追加。 */
class FixedPagePagingSource<Value : Any>(
    private val source: PagingSource<Int, Value>,
    private val page: Int,
) : PagingSource<Int, Value>(), PageInfoReporting {
    override var onPageInfoLoaded: ((totalPages: Int, totalCount: Int) -> Unit)? = null

    init {
        require(page >= 1)
        require(source is PageInfoReporting)
        source.onPageInfoLoaded = { pages, count ->
            if (!invalid) onPageInfoLoaded?.invoke(pages, count)
        }
        source.registerInvalidatedCallback(::invalidate)
        registerInvalidatedCallback(source::invalidate)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> {
        if (invalid) return LoadResult.Invalid()
        return when (val result = source.load(LoadParams.Refresh(
            key = page,
            loadSize = params.loadSize,
            placeholdersEnabled = false,
        ))) {
            is LoadResult.Page -> if (invalid) {
                LoadResult.Invalid()
            } else {
                LoadResult.Page(data = result.data, prevKey = null, nextKey = null)
            }
            is LoadResult.Error -> result
            is LoadResult.Invalid -> result
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Value>): Int = page
}
