package com.shizq.bika.core.data.repository

import androidx.paging.PagingData
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.paging.ChapterMeta
import com.shizq.bika.core.data.paging.ChapterPage
import kotlinx.coroutines.flow.Flow

interface ChapterRepository {
    /**
     * 获取指定漫画的章节列表（分页）。
     */
    fun getChapterList(comicId: String): Flow<PagingData<Chapter>>

    /**
     * 获取指定章节的图片列表（分页），以及随分页请求一并返回的章节元信息。
     */
    fun getChapterPages(comicId: String, order: Int): ChapterPagesResult
}

/**
 * [ChapterRepository.getChapterPages] 的返回结果。
 *
 * 章节图片走 Paging3 分页加载，而标题、总页数等元信息是在拉取图片时由响应体“捎带”出来的，
 * 二者生命周期不同、类型也不同，因此拆成两个独立的 Flow：
 * - [pages] 图片分页流，交给 Paging3 的 UI 层（如 collectAsLazyPagingItems）使用。
 * - [meta] 元信息流，首次加载成功后发出一次；每次调用 getChapterPages 都会得到独立的实例，
 *   不同章节/不同调用之间不会互相覆盖。
 */
data class ChapterPagesResult(
    val pages: Flow<PagingData<ChapterPage>>,
    val meta: Flow<ChapterMeta>
)
