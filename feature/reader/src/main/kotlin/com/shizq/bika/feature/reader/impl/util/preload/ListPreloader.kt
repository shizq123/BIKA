package com.shizq.bika.feature.reader.impl.util.preload

import android.content.Context
import coil3.imageLoader
import coil3.request.ImageRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs

interface PreloadModelProvider<T> {
    fun getPreloadRequest(item: T): ImageRequest?
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
    fun updateWindow(requests: List<ImageRequest>, visibleRequests: List<ImageRequest> = emptyList())
}

internal class CoilPreloadRequestEnqueuer(
    context: Context,
    scope: CoroutineScope,
) : PreloadRequestEnqueuer {
    private val imageLoader = context.imageLoader
    private val queue = PreloadQueue(
        scope = scope,
        keyOf = { request: ImageRequest -> request.diskCacheKey ?: request.data.toString() },
        execute = { request ->
            try {
                imageLoader.execute(request)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                KotlinLogging.logger("ReaderPreload").warn(error) { "预载失败，留给可见页请求重试" }
            }
        },
    )

    override fun updateWindow(requests: List<ImageRequest>, visibleRequests: List<ImageRequest>) {
        queue.update(requests, visibleRequests.mapTo(mutableSetOf()) {
            it.diskCacheKey ?: it.data.toString()
        })
    }

    fun close() = queue.close()
}

/**
 * 按滚动方向预载列表前方（或后方）的若干项。
 *
 * 实例持有 [lastFirstVisibleIndex] 这一跨调用状态，用于判断滚动方向。它必须
 * 随「一段连续的滚动上下文」一起销毁：切章后沿用旧值会让第一次回调把方向判反
 * （新章从第 0 页开始，firstVisible 通常小于上一章的位置，被当成向后滚），
 * 于是首屏之后的页漏掉一轮预载。调用方通过重建实例来重置，见 [PagingPreload]。
 */
class ListPreloader<T>(
    private val dataProvider: PreloadDataProvider<T>,
    private val modelProvider: PreloadModelProvider<T>,
    private val enqueuer: PreloadRequestEnqueuer,
    var maxPreload: Int,
) {
    private var lastFirstVisibleIndex = -1
    private var isScrollingForward = true
    private var previousRequests = emptyMap<Int, ImageRequest>()

    fun onScroll(
        firstVisible: Int,
        lastVisible: Int,
    ) {
        if (firstVisible < 0 || lastVisible < firstVisible) {
            previousRequests = emptyMap()
            enqueuer.updateWindow(emptyList())
            return
        }

        // Image size changes and newly loaded paging data can repeat the same first index.
        // They must not reverse the reading direction or cancel useful forward downloads.
        if (firstVisible != lastFirstVisibleIndex) {
            isScrollingForward = firstVisible > lastFirstVisibleIndex
        }
        val totalCount = dataProvider.itemCount

        val (startIndex, directionAndCount) = if (isScrollingForward) {
            (lastVisible + 1) to maxPreload
        } else {
            (firstVisible - 1) to -maxPreload
        }

        lastFirstVisibleIndex = firstVisible

        if (maxPreload <= 0 || totalCount == 0) {
            previousRequests = emptyMap()
            enqueuer.updateWindow(emptyList())
            return
        }

        preload(totalCount, startIndex, directionAndCount, firstVisible..lastVisible)
    }

    private fun preload(
        itemCount: Int,
        startIndex: Int,
        directionAndCount: Int,
        visibleRange: IntRange,
    ) {
        val step = if (directionAndCount > 0) 1 else -1
        val count = abs(directionAndCount)

        val visibleRequests = previousRequests.filterKeys { it in visibleRange }
        val requests = linkedMapOf<Int, ImageRequest>()
        for (i in 0 until count) {
            val index = startIndex + (i * step)
            if (index in 0 until itemCount) {
                val item = dataProvider.getItem(index) ?: continue
                val request = modelProvider.getPreloadRequest(item) ?: continue
                requests[index] = request
            } else {
                break
            }
        }
        enqueuer.updateWindow(requests.values.toList(), visibleRequests.values.toList())
        previousRequests = visibleRequests + requests
    }
}
