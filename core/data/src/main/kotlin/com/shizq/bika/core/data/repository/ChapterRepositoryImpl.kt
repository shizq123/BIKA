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
 * [ChapterRepositoryImpl.getChapterCatalog] 的页数硬上限。
 *
 * 一页 40 话，500 页足够覆盖任何真实漫画；它的作用是在 `pages` 字段不可信时
 * 给循环一个确定的终点，而不是限制正常数据。
 */
private const val MAX_CATALOG_PAGES = 500

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
                var page: Int? = 1
                var pagesFetched = 0

                while (page != null) {
                    if (pagesFetched >= MAX_CATALOG_PAGES) {
                        logger.warn { "章节目录到达 $MAX_CATALOG_PAGES 页上限，comic=$comicId" }
                        break
                    }
                    val eps = try {
                        network.getComicEpisodes(comicId, page).eps
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // 中途失败保留已拉到的部分：原先由下游 catch 统一 emit(Empty)，
                        // 第 3 页失败会把前 2 页已经交出去的目录清空，
                        // 上下章导航从"知道一部分"退化成"什么都不知道"
                        logger.warn(e) { "章节目录第 $page 页失败，保留已拉取部分 comic=$comicId" }
                        break
                    }
                    collected += eps.docs.map { it.asExternalModel() }
                    pagesFetched++

                    // 复用分页源同一套终止规则：除了 page >= pages，空页也必须停。
                    // 原先只判 page >= pages，pages 虚高时这个 while(true) 不会退出，
                    // 会一直对着空页发请求，每页还 emit 一次带着下游重组
                    page = eps.nextPageKey(page)
                    emit(ChapterCatalog(collected.sortedBy { it.order }, isComplete = page == null))
                }

                // 因上限或失败跳出：目录不完整，isComplete = false 让导航知道边界不可信。
                // 一条都没拿到时也要发射，否则下游一直等在初始值上
                if (page != null || collected.isEmpty()) {
                    emit(ChapterCatalog(collected.sortedBy { it.order }, isComplete = false))
                }
            }
                .shareIn(scope, SharingStarted.WhileSubscribed(30_000), replay = 1)
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
