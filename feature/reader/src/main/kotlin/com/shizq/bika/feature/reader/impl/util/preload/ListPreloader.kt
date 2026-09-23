package com.shizq.bika.feature.reader.impl.util.preload

import android.content.Context
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope

interface PreloadModelProvider<T> {
    fun getPreloadRequest(item: T): ImageRequest?

    /** 阅读会话内的稳定页面身份；不能使用列表下标。 */
    fun getPreloadKey(item: T, request: ImageRequest): String =
        request.diskCacheKey ?: request.data.toString()
}


interface PreloadDataProvider<T> {
    /** 列表中的项目总数。 */
    val itemCount: Int

    /** 获取指定位置的项目，如果该位置不可用，则返回 null。 */
    fun getItem(index: Int): T?
}

/**
 * 预载请求的投递出口。
 *
 * 抽成接口是为了让 [ListPreloader] 的滚动方向判定与窗口计算可以脱离 Android
 * Context 单测——这部分逻辑决定了「往哪个方向、预载哪几页」，写错的表现是
 * 静默少预载（用户侧只感觉偶尔卡顿），线上很难察觉。
 */
interface PreloadRequestEnqueuer {
    fun updateWindow(
        requests: List<PreloadRequest>,
        visibleRequests: List<PreloadRequest> = emptyList(),
    )
}

data class PreloadRequest(
    val index: Int,
    val key: String,
    val request: ImageRequest,
)


internal class CoilPreloadRequestEnqueuer(
    context: Context,
    scope: CoroutineScope,
) : PreloadRequestEnqueuer {
    private val imageLoader = context.imageLoader
    private val queue = PreloadQueue(
        scope = scope,
        keyOf = { request: PreloadRequest -> request.key },
        execute = { request ->
            try {
                when (imageLoader.execute(request.request)) {
                    is SuccessResult -> true
                    is ErrorResult -> false
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                KotlinLogging.logger("ReaderPreload").warn(error) { "预载失败，留给可见页请求重试" }
                false
            }
        },
    )

    override fun updateWindow(
        requests: List<PreloadRequest>,
        visibleRequests: List<PreloadRequest>,
    ) {
        queue.update(
            items = requests,
            retainRunning = visibleRequests.mapTo(mutableSetOf(), PreloadRequest::key),
        )
    }

    fun close() = queue.close()
}

/**
 * 预载窗口由 [PreloadPlanner] 计算，任务身份由 [PreloadModelProvider.getPreloadKey] 提供。
 * 下标只用于从 Paging 数据源取出项目，不再作为队列去重身份。

 */
class ListPreloader<T>(
    private val dataProvider: PreloadDataProvider<T>,
    private val modelProvider: PreloadModelProvider<T>,
    private val enqueuer: PreloadRequestEnqueuer,
    var maxPreload: Int,
) {
    private var lastFirstVisibleIndex = -1
    private var isScrollingForward = true
    private var previousRequests = emptyMap<String, PreloadRequest>()
    private val planner = PreloadPlanner()


    internal fun reset() {
        lastFirstVisibleIndex = -1
        isScrollingForward = true
        planner.reset()
        previousRequests = emptyMap()
        enqueuer.updateWindow(emptyList())
    }

    internal fun onViewport(snapshot: ViewportSnapshot) {
        if (snapshot.visibleRange == null || maxPreload <= 0 || dataProvider.itemCount <= 0) {
            previousRequests = emptyMap()
            enqueuer.updateWindow(emptyList())
            return
        }

        val plan = planner.plan(
            viewport = snapshot,
            preloadCount = maxPreload,
            itemCount = dataProvider.itemCount,
        )
        preload(
            indices = plan.indices,
            visibleRange = snapshot.visibleRange,
        )
    }

    fun onScroll(
        firstVisible: Int,
        lastVisible: Int,
    ) {
        if (firstVisible < 0 || lastVisible < firstVisible) {
            previousRequests = emptyMap()
            enqueuer.updateWindow(emptyList())
            return
        }

        if (firstVisible != lastFirstVisibleIndex) {
            isScrollingForward = firstVisible > lastFirstVisibleIndex
        }
        lastFirstVisibleIndex = firstVisible

        onViewport(
            ViewportSnapshot(
                visibleRange = firstVisible..lastVisible,
                direction = if (isScrollingForward) {
                    ScrollDirection.Forward
                } else {
                    ScrollDirection.Backward
                },
                cause = ViewportChangeCause.UserScroll,
            )
        )
    }


    private fun preload(
        indices: List<Int>,
        visibleRange: IntRange,
    ) {
        val visibleRequests = previousRequests.values
            .filter { it.index in visibleRange }
            .associateByTo(linkedMapOf(), PreloadRequest::key)
        val requests = linkedMapOf<String, PreloadRequest>()
        for (index in indices) {
            val item = dataProvider.getItem(index) ?: continue
            val request = modelProvider.getPreloadRequest(item) ?: continue
            val preloadRequest = PreloadRequest(
                index = index,
                key = modelProvider.getPreloadKey(item, request),
                request = request,
            )
            requests[preloadRequest.key] = preloadRequest
        }
        enqueuer.updateWindow(requests.values.toList(), visibleRequests.values.toList())
        previousRequests = visibleRequests + requests
    }

}
