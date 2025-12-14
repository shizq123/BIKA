package com.shizq.bika.ui.reader.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.paging.compose.LazyPagingItems
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import com.shizq.bika.paging.ChapterPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 高性能、方向感知的图片预加载器，不直接依赖 LazyListState。
 *
 * @param imageList Paging 数据源
 * @param firstVisibleIndex 当前可见的第一个 item 的索引
 * @param preloadCount 每个方向上预加载的数量
 */
@Composable
fun ImagePreloader(
    imageList: LazyPagingItems<ChapterPage>,
    firstVisibleIndex: Int,
    preloadCount: Int,
    fixedVisibleItemCount: Int = 1
) {
    val context = LocalContext.current
    val totalItemCount = imageList.itemCount

    var lastFirstVisibleIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(firstVisibleIndex, totalItemCount) {
        delay(200)

        if (totalItemCount == 0) {
            lastFirstVisibleIndex = -1
            return@LaunchedEffect
        }

        val isScrollingForward = firstVisibleIndex > lastFirstVisibleIndex
        val isScrollingBackward = firstVisibleIndex < lastFirstVisibleIndex

        val urlsToPreload = when {
            isScrollingBackward -> {
                // 向后滚动 (向上/向左): 预加载前面的图片
                val startIndex = firstVisibleIndex - fixedVisibleItemCount
                getUrlsInRange(imageList, totalItemCount, startIndex, -preloadCount)
            }

            isScrollingForward -> {
                // 向前滚动 (向下/向右) 或 首次加载: 预加载后面的图片
                val startIndex = firstVisibleIndex + fixedVisibleItemCount
                getUrlsInRange(imageList, totalItemCount, startIndex, preloadCount)
            }

            else -> {
                // 索引未变化（已经被上面处理），此分支理论上不会走到
                emptyList()
            }
        }

        if (urlsToPreload.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                preloadImagesWithCoil(context, urlsToPreload, Size.ORIGINAL)
            }
        }

        lastFirstVisibleIndex = firstVisibleIndex
    }
}

/**
 * 辅助函数，从 PagingItems 中安全地提取 URL 列表
 * @param directionAndCount 正数表示向后取，负数表示向前取
 */
private fun getUrlsInRange(
    imageList: LazyPagingItems<ChapterPage>,
    itemCount: Int,
    startIndex: Int,
    directionAndCount: Int
): List<String> {
    val urls = mutableListOf<String>()

    if (directionAndCount > 0) { // Forward
        for (i in 0 until directionAndCount) {
            val index = startIndex + i
            if (index in 0 until itemCount) {
                imageList.peek(index)?.url?.let { urls.add(it) }
            } else {
                break
            }
        }
    } else { // Backward
        for (i in 0 until -directionAndCount) {
            val index = startIndex - i
            if (index in 0 until itemCount) {
                imageList.peek(index)?.url?.let { urls.add(it) }
            } else {
                break
            }
        }
    }
    return urls
}

/**
 * 使用 Coil 预加载图片列表到缓存
 *
 * @param context Context
 * @param urls 要预加载的 URL 列表
 * @param size 预加载的目标尺寸
 */
private fun preloadImagesWithCoil(context: Context, urls: List<String>, size: Size) {
    val imageLoader = context.imageLoader
    urls.forEach { url ->
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(size)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .allowHardware(true)
            .build()

        imageLoader.enqueue(request)
    }
}