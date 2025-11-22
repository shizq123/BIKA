package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders

class ChapterListPagingSource(
    private val id: String,
) : PagingSource<Int, Chapter>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Chapter> {
        return try {
            val currentPage = params.key ?: 1
            val response = RetrofitUtil.service.comicsEpisodeGet2(
                id,
                currentPage,
                BaseHeaders("comics/$id/eps?page=$currentPage", "GET").getHeaderMapAndToken()
            )
            val data = response.data ?: return LoadResult.Error(Exception("Data is null"))
            if (response.code != 200) {
                return LoadResult.Error(Exception(response.message))
            }

            LoadResult.Page(
                data = data.eps.docs.map { doc ->
                    Chapter(doc.id, doc.order, doc.title, doc.updatedAt)
                },
                prevKey = null,
                nextKey = if (currentPage < data.eps.pages) currentPage + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }

    }

    override fun getRefreshKey(state: PagingState<Int, Chapter>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}

data class Chapter(
    val id: String,
    val order: Int,
    val title: String,
    val updatedAt: String
)