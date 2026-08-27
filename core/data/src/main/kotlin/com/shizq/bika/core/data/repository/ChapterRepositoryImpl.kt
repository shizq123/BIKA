package com.shizq.bika.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.model.ChapterCatalog
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.data.paging.ChapterListPagingSource
import com.shizq.bika.core.data.paging.ChapterMeta
import com.shizq.bika.core.data.paging.ChapterPagesPagingSource
import com.shizq.bika.core.network.BikaDataSource
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow

private const val CHAPTER_LIST_PAGE_SIZE = 20
private const val CHAPTER_PAGES_PAGE_SIZE = 40

class ChapterRepositoryImpl @Inject constructor(
    private val chapterListPagingSourceFactory: ChapterListPagingSource.Factory,
    private val chapterPagesPagingSourceFactory: ChapterPagesPagingSource.Factory,
    private val network: BikaDataSource,
) : ChapterRepository {

    override fun getChapterList(comicId: String): Flow<PagingData<Chapter>> =
        Pager(PagingConfig(pageSize = CHAPTER_LIST_PAGE_SIZE)) {
            chapterListPagingSourceFactory.create(comicId)
        }.flow

    override fun getChapterCatalog(comicId: String): Flow<ChapterCatalog> = flow {
        val collected = mutableListOf<Chapter>()
        var page = 1
        while (true) {
            val response = network.getComicEpisodes(comicId, page).eps
            collected += response.docs.map { it.asExternalModel() }
            val isComplete = page >= response.pages
            emit(
                ChapterCatalog(
                    chapters = collected.sortedBy { it.order },
                    isComplete = isComplete
                )
            )
            if (isComplete) break
            page++
        }
    }

    override fun getChapterPages(comicId: String, order: Int): ChapterPagesResult {
        // 局部变量：仅归属于这一次调用，不同章节/不同调用互不影响，避免共享状态污染
        val metadata = MutableStateFlow<ChapterMeta?>(null)

        val pages = Pager(PagingConfig(pageSize = CHAPTER_PAGES_PAGE_SIZE)) {
            chapterPagesPagingSourceFactory.create(comicId, order, metadata)
        }.flow

        return ChapterPagesResult(
            pages = pages,
            meta = metadata.filterNotNull()
        )
    }
}
