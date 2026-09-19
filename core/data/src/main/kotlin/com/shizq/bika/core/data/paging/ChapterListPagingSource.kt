package com.shizq.bika.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.nextPageKey
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * 漫画章节列表的分页源，`comics/{id}/eps` 的唯一分页入口。
 *
 * 原先 app 模块另有一个 EpisodePagingSource 打同一个端点、做同一件事，
 * 区别只是产物用网络模型 Episode 而这里用领域模型 Chapter。两份实现各自维护
 * 去重与翻页，已经出现分叉（那边有跨页去重，这边没有）。现在只留这一个。
 */
class ChapterListPagingSource @AssistedInject constructor(
    @Assisted private val id: String,
    private val network: BikaDataSource
) : PagingSource<Int, Chapter>() {

    /**
     * 跨页重复的 id 在这里拦掉，UI 的 key 才能回归纯 id。
     *
     * 章节列表同样会抖动：新章节发布后旧章节被挤到下一页，
     * 同一条落在两页就是 LazyVerticalGrid 的 "Key was already used" 崩溃。
     */
    private val deduplicator = CrossPageDeduplicator<Chapter> { it.id }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Chapter> {
        val currentPage = params.key ?: 1

        return try {
            val epsResponse = network.getComicEpisodes(id, currentPage).eps
            val chapters = epsResponse.docs.map { it.asExternalModel() }

            LoadResult.Page(
                data = deduplicator.retainUnseen(currentPage, chapters).sortedBy { it.order },
                prevKey = null,
                // 判空用的是接口原始响应而非去重后的结果：整页都是重复项时
                // 页本身是有数据的，用去重结果判空会提前终止、丢掉后面的章节
                nextKey = epsResponse.nextPageKey(currentPage),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Chapter>): Int? = null

    @AssistedFactory
    interface Factory {
        fun create(id: String): ChapterListPagingSource
    }
}
