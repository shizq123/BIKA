package com.shizq.bika.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders

class ComicPagingSource(
    val id: String,
    val order: Int,
) : PagingSource<Int, ComicPage>() {
    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, ComicPage> {
        return try {
            val currentPage = params.key ?: 1
            val response = RetrofitUtil.service.comicsPictureGet2(
                id, order, currentPage,
                BaseHeaders(
                    "comics/$id/order/$order/pages?page=$currentPage",
                    "GET"
                ).getHeaderMapAndToken()
            )
            val data = response.data ?: return LoadResult.Error(Exception("Data is null"))
            if (response.code != 200) {
                return LoadResult.Error(Exception(response.message))
            }
            val pageInfo = data.pages
            LoadResult.Page(
                data = pageInfo.docs.map { doc ->
                    // 处理 URL 拼接
                    val fileServer = doc.media.fileServer
                    val path = doc.media.path
                    val fullUrl =
                        if (fileServer.endsWith("/")) "${fileServer}static/$path" else "$fileServer/static/$path"

                    ComicPage(id = doc.id, url = fullUrl)
                },
                prevKey = if (currentPage == 1) null else currentPage - 1,
                nextKey = if (currentPage < pageInfo.pages) currentPage + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ComicPage>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}

data class ComicPage(
    val id: String,
    val url: String,
)