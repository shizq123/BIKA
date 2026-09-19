package com.shizq.bika.ui.comicinfo.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.data.model.Comment
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.Type
import com.shizq.bika.core.network.model.nextPageKey
import com.shizq.bika.paging.CrossPageDeduplicator
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * 漫画评论列表的分页源，只负责分页。
 *
 * 置顶评论不在这里：它不分页、与页码无关，由 ViewModel 单独请求
 * （见 ComicInfoViewModel.pinnedComments）。早前的实现让本类在第一页
 * 成功时旁路回调置顶评论，带来三个无法在本类内解决的问题：失败分支会清空
 * 已显示的置顶评论、`cachedIn` 重放缓存页时 `load` 不再执行导致置顶评论丢失、
 * 以及 refresh 后首个 `page` 并非 1 时置顶评论不再更新。
 */
class CommentPagingSource @AssistedInject constructor(
    private val apiService: BikaDataSource,
    @Assisted private val comicId: String,
) : PagingSource<Int, Comment>() {

    /** 跨页重复的 id 在这里拦掉，UI 的 key 才能回归纯 id */
    private val deduplicator = CrossPageDeduplicator<Comment> { it.id }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comment> {
        val page = params.key ?: 1

        return try {
            val commentsPage = apiService.getComments(Type.COMIC, comicId, page).comments

            LoadResult.Page(
                data = deduplicator.retainUnseen(
                    page,
                    commentsPage.docs.map { it.asExternalModel() }
                ),
                prevKey = null,
                nextKey = commentsPage.nextPageKey(page),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Comment>): Int? = null

    @AssistedFactory
    interface Factory {
        operator fun invoke(comicId: String): CommentPagingSource
    }
}
