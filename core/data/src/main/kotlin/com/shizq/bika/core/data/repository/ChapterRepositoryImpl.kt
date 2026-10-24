package com.shizq.bika.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.shizq.bika.core.coroutine.ApplicationScope
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.model.ChapterCatalog
import com.shizq.bika.core.data.paging.ChapterListPagingSource
import com.shizq.bika.core.data.paging.ChapterMeta
import com.shizq.bika.core.data.paging.ChapterPagesPagingSource
import com.shizq.bika.core.data.paging.loadAllChapters
import com.shizq.bika.core.data.paging.walkEpisodePages
import com.shizq.bika.core.network.BikaDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
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
                    fetchPage = { page -> network.getComicEpisodes(comicId, page).eps },
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

                // 连第一页都没成功：一条都没发过，下游会一直等在初始值上。
                // 同时从缓存中移除，避免网络偶发失败后永久锁死为空目录
                if (!emittedAny) {
                    catalogCache.remove(comicId)
                    emit(ChapterCatalog.Empty)
                }
            }
                .shareIn(scope, SharingStarted.WhileSubscribed(30_000), replay = 1)
        }

    override suspend fun getAllChapters(comicId: String): List<Chapter> =
        loadAllChapters { page -> network.getComicEpisodes(comicId, page).eps }

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
