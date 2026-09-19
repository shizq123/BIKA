package com.shizq.bika.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.model.ChapterCatalog
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.data.paging.ChapterListPagingSource
import com.shizq.bika.core.data.paging.ChapterMeta
import com.shizq.bika.core.data.paging.ChapterPagesPagingSource
import com.shizq.bika.core.network.BikaDataSource
import com.shizq.bika.core.network.model.nextPageKey
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import java.util.concurrent.ConcurrentHashMap

private const val CHAPTER_LIST_PAGE_SIZE = 20
private const val CHAPTER_PAGES_PAGE_SIZE = 40

/**
 * 全量拉取章节时的页数硬上限，[getChapterCatalog] 与 [getAllChapters] 共用。
 *
 * 一页 40 话，500 页足够覆盖任何真实漫画；它的作用是在 `pages` 字段不可信时
 * 给循环一个确定的终点，而不是限制正常数据。
 *
 * 原先 ViewModel 侧另有一份同值同用途的 MAX_EPISODE_PAGES，两处都为同一个端点
 * 兜底却各自定义；现在全量拉取只剩这一条路径，常量也只留这一份。
 */
private const val MAX_EPISODE_FETCH_PAGES = 500

private val logger = KotlinLogging.logger("ChapterRepository")

class ChapterRepositoryImpl @Inject constructor(
    private val chapterListPagingSourceFactory: ChapterListPagingSource.Factory,
    private val chapterPagesPagingSourceFactory: ChapterPagesPagingSource.Factory,
    private val network: BikaDataSource,
    @ApplicationScope private val scope: CoroutineScope,
) : ChapterRepository {
    private val catalogCache = ConcurrentHashMap<String, Flow<ChapterCatalog>>()
    override fun getChapterList(comicId: String): Flow<PagingData<Chapter>> =
        Pager(PagingConfig(pageSize = CHAPTER_LIST_PAGE_SIZE)) {
            chapterListPagingSourceFactory.create(comicId)
        }.flow

    override fun getChapterCatalog(comicId: String): Flow<ChapterCatalog> =
        catalogCache.getOrPut(comicId) {
            flow {
                val collected = mutableListOf<Chapter>()
                var emittedAny = false

                walkEpisodePages(
                    comicId = comicId,
                    onPage = { chapters, isLastPage ->
                        collected += chapters
                        emittedAny = true
                        // isComplete 直接取"是否末页"：因页数上限或中途失败停下时
                        // 这个值是 false，导航据此知道边界不可信
                        emit(
                            ChapterCatalog(
                                chapters = collected.sortedBy { it.order },
                                isComplete = isLastPage,
                            )
                        )
                    },
                    // 中途失败保留已拉到的部分：若由下游 catch 统一 emit(Empty)，
                    // 第 3 页失败会把前 2 页已经交出去的目录清空，
                    // 上下章导航从"知道一部分"退化成"什么都不知道"
                    onPageError = { page, e ->
                        logger.warn(e) { "章节目录第 $page 页失败，保留已拉取部分 comic=$comicId" }
                    },
                )

                // 连第一页都没成功：一条都没发过，下游会一直等在初始值上
                if (!emittedAny) {
                    emit(ChapterCatalog.Empty)
                }
            }
                .shareIn(scope, SharingStarted.WhileSubscribed(30_000), replay = 1)
        }

    override suspend fun getAllChapters(comicId: String): List<Chapter> {
        val collected = mutableListOf<Chapter>()

        walkEpisodePages(
            comicId = comicId,
            onPage = { chapters, _ -> collected += chapters },
            // 不吞异常：下载选择面板拿到残缺列表，用户会以为章节就这么多
            onPageError = { _, e -> throw e },
        )

        return collected.sortedBy { it.order }
    }

    /**
     * 逐页走完 `comics/{id}/eps`，每页交给 [onPage]。
     *
     * 抽出来是因为"全量拉章节"有两个交付形态（目录流、一次性列表），
     * 但翻页规则、页数上限、空页终止这些约束必须完全一致——原先两份实现
     * 各写一遍，连页数上限都是两个同值常量。
     *
     * @param onPage 第二个参数表示本页是否为末页。目录流用它决定 isComplete，
     *   因页数上限或单页失败提前终止时最后一次回调收到的是 false
     * @param onPageError 单页失败时的处置。目录流选择"记日志并停下、保留已拉部分"，
     *   一次性列表选择"抛出去"，差异由调用方决定；抛出即终止整个遍历
     */
    private suspend fun walkEpisodePages(
        comicId: String,
        onPage: suspend (chapters: List<Chapter>, isLastPage: Boolean) -> Unit,
        onPageError: (page: Int, e: Exception) -> Unit,
    ) {
        var page: Int? = 1
        var pagesFetched = 0

        while (page != null) {
            if (pagesFetched >= MAX_EPISODE_FETCH_PAGES) {
                logger.warn { "章节拉取到达 $MAX_EPISODE_FETCH_PAGES 页上限，comic=$comicId" }
                return
            }
            val eps = try {
                network.getComicEpisodes(comicId, page).eps
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onPageError(page, e)
                return
            }
            pagesFetched++

            // 与分页源同一套终止规则：除了 page >= pages，空页也必须停。
            // 只判 page >= pages 的话，pages 虚高时循环不会退出，会一直对着空页发请求
            val nextPage = eps.nextPageKey(page)
            onPage(eps.docs.map { it.asExternalModel() }, nextPage == null)
            page = nextPage
        }
    }

    override fun getChapterPages(
        comicId: String,
        order: Int,
        startPageIndex: Int
    ): ChapterPagesResult {
        // 局部变量：仅归属于这一次调用，不同章节/不同调用互不影响，避免共享状态污染
        val metadata = MutableStateFlow<ChapterMeta?>(null)

        // 索引 -> API 页的换算依赖服务端每页数量，但首次请求前这个值还不知道，
        // 只能用 CHAPTER_PAGES_PAGE_SIZE 作为猜测（与 PagingConfig.pageSize 保持一致）。
        // 猜错的后果是首次加载没有精确落在目标索引所在页，getRefreshKey 会在数据到位后
        // 用真实 limit 重新算一次，不会导致崩溃或死循环，只是首屏多一次纠偏。
        val initialApiPage = (startPageIndex / CHAPTER_PAGES_PAGE_SIZE) + 1

        val pages = Pager(
            config = PagingConfig(
                pageSize = CHAPTER_PAGES_PAGE_SIZE,
                enablePlaceholders = true,
            ),
            initialKey = initialApiPage.takeIf { it > 1 },
        ) {
            chapterPagesPagingSourceFactory.create(comicId, order, initialApiPage, metadata)
        }.flow

        return ChapterPagesResult(
            pages = pages,
            meta = metadata.filterNotNull()
        )
    }
}
