package com.shizq.bika.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import com.shizq.bika.core.network.BikaDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

private val logger = KotlinLogging.logger("ChapterPagesPagingSource")

class ChapterPagesPagingSource @AssistedInject constructor(
    @Assisted private val id: String,
    @Assisted("order") private val order: Int,
    @Assisted("initialApiPage") private val initialApiPage: Int,
    @Assisted private val metadata: MutableStateFlow<ChapterMeta?>,
    private val dataSource: BikaDataSource
) : PagingSource<Int, ChapterPage>() {

    override val jumpingSupported: Boolean = true

    @Volatile
    private var lastKnownLimit: Int? = null

    @Volatile
    private var lastKnownPages: Int? = null

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, ChapterPage> {
        // initialApiPage 只在 REFRESH 且尚无 key 时生效（即 initial load）；
        // append/prepend 沿用 Paging 传入的 key，不受它影响。
        val requestedApiPage = params.key ?: initialApiPage

        return try {
            val response = dataSource.getChapterPages(id, order, requestedApiPage)
            val imagePages = response.imagePages
            val respPage = imagePages.page
            val limit = imagePages.limit
            val total = imagePages.total
            val docsSize = imagePages.docs.size

            if (respPage != requestedApiPage) {
                logger.warn {
                    "服务端返回页与请求页不一致: 请求=$requestedApiPage 实际=$respPage " +
                            "(可能是章节已缩水，或服务端对超范围请求做了 clamp)"
                }
            }

            // 中间出现空页但按 total 计算后面还应该有数据：数据源自相矛盾，
            // 不生成用户能看见但无法通过下拉重试的空洞，直接报错走现有重试 UI。
            if (docsSize == 0 && limit > 0 && total > respPage * limit) {
                return LoadResult.Error(
                    IllegalStateException(
                        "章节分页返回空页但 total 显示后面仍有数据: page=$respPage total=$total limit=$limit"
                    )
                )
            }

            metadata.value = ChapterMeta(title = response.chapterInfo.title, totalImages = total)
            lastKnownLimit = limit.takeIf { it > 0 }
            lastKnownPages = imagePages.pages.takeIf { it > 0 }

            val (itemsBefore, itemsAfter) = computePlaceholderCounts(
                respPage = respPage,
                limit = limit,
                total = total,
                docsSize = docsSize,
            )

            LoadResult.Page(
                data = imagePages.docs.map { image ->
                    ChapterPage(id = image.imageId, url = image.media.originalImageUrl)
                },
                prevKey = if (respPage > 1) respPage - 1 else null,
                nextKey = if (respPage < imagePages.pages) respPage + 1 else null,
                itemsBefore = itemsBefore,
                itemsAfter = itemsAfter,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "章节分页加载失败: comic=$id order=$order 请求页=$requestedApiPage" }
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ChapterPage>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchorPosition) ?: return null

        // 分支 A：锚点落在某个已加载页自身的绝对范围内——用该页自己的 key，
        // 对 limit 是否漂移天然免疫，是普通滚动触发 refresh 的正常路径。
        val itemsBefore = anchorPage.itemsBefore
        if (itemsBefore != LoadResult.Page.COUNT_UNDEFINED) {
            val rangeEnd = itemsBefore + anchorPage.data.size
            if (anchorPosition in itemsBefore until rangeEnd) {
                return anchorPage.currentApiPage()
            }
        }

        // 分支 B：远跳（恢复进度、拖动进度条），锚点在已加载窗口之外，只能靠 limit 反推。
        val limit = lastKnownLimit
        if (limit == null || limit <= 0) {
            return anchorPage.currentApiPage()
        }

        val computedPage = anchorPosition / limit + 1
        val upperBound = lastKnownPages ?: computedPage
        return computedPage.coerceIn(1, upperBound)
    }

    private fun LoadResult.Page<Int, ChapterPage>.currentApiPage(): Int? =
        prevKey?.plus(1) ?: nextKey?.minus(1)

    @AssistedFactory
    interface Factory {
        fun create(
            id: String,
            @Assisted("order") order: Int,
            @Assisted("initialApiPage") initialApiPage: Int,
            metadata: MutableStateFlow<ChapterMeta?>,
        ): ChapterPagesPagingSource
    }
}

/**
 * itemsBefore/itemsAfter 换算结果。任一字段为 [LoadResult.Page.COUNT_UNDEFINED]
 * 时应成对退化——不允许一个可信、另一个不可信，否则 itemCount 在该页前后会不一致。
 */
internal data class PlaceholderCounts(val itemsBefore: Int, val itemsAfter: Int)

/**
 * 根据单次响应自身的 respPage/limit/total/docsSize 计算 itemsBefore/itemsAfter。
 *
 * 所有入参均来自 [com.shizq.bika.core.network.model.PageData]，其字段套了
 * [com.shizq.bika.core.network.utils.LenientIntSerializer]（服务端偶发返回非整数），
 * 不可假设它们互相自洽，因此每一步都做边界钳制，绝不向 Paging3 传负数
 * （负数会直接抛 IllegalArgumentException，比"placeholder 数量不准"严重得多）。
 */
internal fun computePlaceholderCounts(
    respPage: Int,
    limit: Int,
    total: Int,
    docsSize: Int,
): PlaceholderCounts {
    if (limit <= 0 || total <= 0 || respPage <= 0) {
        return PlaceholderCounts(LoadResult.Page.COUNT_UNDEFINED, LoadResult.Page.COUNT_UNDEFINED)
    }

    val itemsBeforeRaw = (respPage - 1) * limit
    if (itemsBeforeRaw < 0) {
        logger.warn { "itemsBefore 计算为负: respPage=$respPage limit=$limit，字段不可信" }
        return PlaceholderCounts(LoadResult.Page.COUNT_UNDEFINED, LoadResult.Page.COUNT_UNDEFINED)
    }

    val itemsAfterRaw = total - itemsBeforeRaw - docsSize
    val itemsAfter = if (itemsAfterRaw < 0) {
        logger.warn { "itemsAfter 计算为负: total=$total itemsBefore=$itemsBeforeRaw docsSize=$docsSize" }
        0
    } else itemsAfterRaw

    return PlaceholderCounts(itemsBeforeRaw, itemsAfter)
}

data class ChapterMeta(
    val title: String,
    val totalImages: Int
)

data class ChapterPage(
    val id: String,
    val url: String,
)