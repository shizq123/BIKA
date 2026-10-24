package com.shizq.bika.core.data.paging

import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.data.model.asExternalModel
import com.shizq.bika.core.network.model.Episode
import com.shizq.bika.core.network.model.PageData
import com.shizq.bika.core.network.model.nextPageKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException

internal const val MAX_EPISODE_FETCH_PAGES = 500

internal suspend fun loadAllChapters(
    fetchPage: suspend (Int) -> PageData<Episode>,
): List<Chapter> {
    val collected = mutableListOf<Chapter>()
    walkEpisodePages(
        fetchPage = fetchPage,
        onPage = { chapters, _ -> collected += chapters },
        onPageError = { _, error -> throw error },
    )
    return collected.sortedBy { it.order }
}

/** 目录流允许保留已加载部分；完整目录通过 [onPageError] 抛出错误，避免交付残缺结果。 */
internal suspend fun walkEpisodePages(
    fetchPage: suspend (Int) -> PageData<Episode>,
    onPage: suspend (chapters: List<Chapter>, isLastPage: Boolean) -> Unit,
    onPageError: (page: Int, error: Exception) -> Unit,
) {
    var page: Int? = 1
    val seenIds = HashSet<String>()

    while (page != null) {
        currentCoroutineContext().ensureActive()
        if (page > MAX_EPISODE_FETCH_PAGES) {
            onPageError(page, IOException("章节目录超过 $MAX_EPISODE_FETCH_PAGES 页，未完成加载"))
            return
        }
        val eps = try {
            fetchPage(page)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onPageError(page, e)
            return
        }
        currentCoroutineContext().ensureActive()

        // 整页重复仍需继续翻页，结束条件必须取接口原始响应。
        val nextPage = eps.nextPageKey(page)
        val chapters = eps.docs
            .filter { seenIds.add(it.id) }
            .map { it.asExternalModel() }
        onPage(chapters, nextPage == null)
        page = nextPage
    }
}
