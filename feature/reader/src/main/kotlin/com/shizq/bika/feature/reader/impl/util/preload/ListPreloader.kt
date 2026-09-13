package com.shizq.bika.feature.reader.impl.util.preload

import android.content.Context
import coil3.imageLoader
import coil3.request.ImageRequest
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
    fun enqueue(request: ImageRequest)
}

internal class CoilPreloadRequestEnqueuer(
    private val context: Context,
) : PreloadRequestEnqueuer {
    override fun enqueue(request: ImageRequest) {
        context.imageLoader.enqueue(request)
    }
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
    private val maxPreload: Int,
) {
    private var lastFirstVisibleIndex = -1

    fun onScroll(
        firstVisible: Int,
        lastVisible: Int,
    ) {
        if (lastVisible < 0) return

        val isScrollingForward = firstVisible > lastFirstVisibleIndex
        val totalCount = dataProvider.itemCount

        val (startIndex, directionAndCount) = if (isScrollingForward) {
            (lastVisible + 1) to maxPreload
        } else {
            (firstVisible - 1) to -maxPreload
        }

        lastFirstVisibleIndex = firstVisible

        if (directionAndCount == 0 || totalCount == 0) return

        preload(totalCount, startIndex, directionAndCount)
    }

    private fun preload(
        itemCount: Int,
        startIndex: Int,
        directionAndCount: Int,
    ) {
        val step = if (directionAndCount > 0) 1 else -1
        val count = abs(directionAndCount)

        for (i in 0 until count) {
            val index = startIndex + (i * step)
            if (index in 0 until itemCount) {
                val item = dataProvider.getItem(index) ?: continue
                val request = modelProvider.getPreloadRequest(item) ?: continue
                enqueuer.enqueue(request)
            } else {
                break
            }
        }
    }
}
