package com.shizq.bika.core.data.repository

import androidx.paging.PagingData
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.model.ChapterCatalog
import com.shizq.bika.core.data.paging.ChapterMeta
import com.shizq.bika.core.data.paging.ChapterPage
import kotlinx.coroutines.flow.Flow

interface ChapterRepository {
    /**
     * 获取指定漫画的章节列表（分页）。
     */
    fun getChapterList(comicId: String): Flow<PagingData<Chapter>>

    /**
     * 获取指定漫画的完整章节目录（非分页，全量）。
     *
     * 用于上下章导航：导航需要随机访问任意章节的相邻项，分页窗口无法满足
     * （例如从历史记录直接进入第 35 话时，分页首屏只有前 20 条）。
     * 内部按页循环拉取直至拉全，每拉完一页即发射一次当前已知目录，
     * [ChapterCatalog.isComplete] 标记是否已拉取完毕。
     */
    fun getChapterCatalog(comicId: String): Flow<ChapterCatalog>

    /**
     * 一次性取回指定漫画的全部章节（非分页、非 Flow）。
     *
     * 与 [getChapterCatalog] 的区别只在交付方式：目录是长期订阅、边拉边发、带
     * [ChapterCatalog.isComplete] 语义，供上下章导航随机访问；这里是一次 suspend
     * 调用拿到最终结果，供"下载选择"这类打开面板时才需要全量的一次性场景。
     * 两者共用同一套翻页与上限规则。
     *
     * 中途失败不吞：直接抛给调用方，由 UI 决定是否提示重试。这与目录流刻意不同——
     * 目录流保留已拉到的部分是因为"知道一部分"也比"什么都不知道"强，
     * 而下载选择面板拿到残缺列表会让用户以为章节就这么多。
     */
    suspend fun getAllChapters(comicId: String): List<Chapter>

    /**
     * 获取指定章节的图片列表（分页），以及随分页请求一并返回的章节元信息。
     *
     * @param startPageIndex 恢复阅读进度时的目标页索引（0-based，对应 [ChapterPage] 在
     *   全章中的绝对位置）。传入非 0 值时，首次加载会直接请求覆盖该索引的 API 页，
     *   而不是总是从第 1 页开始——避免恢复到很靠后的页时需要逐页向前加载。
     *   换算依赖服务端每页数量，若不确定可传 0（等价于原有行为）。
     */
    fun getChapterPages(comicId: String, order: Int, startPageIndex: Int = 0): ChapterPagesResult
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
