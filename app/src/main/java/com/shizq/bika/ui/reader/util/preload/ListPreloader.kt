package com.shizq.bika.ui.reader.util.preload

import android.content.Context
import coil3.imageLoader
import coil3.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

class ListPreloader<T>(
    private val context: Context,
    private val dataProvider: PreloadDataProvider<T>,
    private val modelProvider: PreloadModelProvider<T>,
    private val maxPreload: Int
) {
    private var lastFirstVisibleIndex = -1

    fun onScroll(
        scope: CoroutineScope,
        firstVisible: Int,
        lastVisible: Int
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

        scope.launch(Dispatchers.IO) {
            preload(totalCount, startIndex, directionAndCount)
        }
    }

    private fun preload(
        itemCount: Int,
        startIndex: Int,
        directionAndCount: Int
    ) {
        val imageLoader = context.imageLoader
        val step = if (directionAndCount > 0) 1 else -1
        val count = abs(directionAndCount)

        for (i in 0 until count) {
            val index = startIndex + (i * step)
            if (index in 0 until itemCount) {
                val item = dataProvider.getItem(index) ?: continue
                val request = modelProvider.getPreloadRequest(item) ?: continue
                imageLoader.enqueue(request)
            } else {
                break
            }
        }
    }
}