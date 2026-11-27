package com.shizq.bika.feature.reader.impl.util.preload

import android.content.Context
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Dimension
import coil3.size.Size
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.core.network.plugin.preloadRequest

class ChapterPagePreloadProvider(private val context: Context) : PreloadModelProvider<ChapterPage> {
    // 与 ComicItem 保持一致：宽度限制在 2 倍屏幕宽度，防止超宽/超长原图解码进内存导致 OOM
    private val preloadDecodeWidthPx =
        (context.resources.displayMetrics.widthPixels * 2).coerceAtLeast(1080)

    override fun getPreloadKey(item: ChapterPage, request: ImageRequest): String =
        "chapter-page:${item.id}:${item.url}"

    override fun getPreloadRequest(item: ChapterPage): ImageRequest {
        return ImageRequest.Builder(context)
            .data(item.url)
            .size(Size(Dimension(preloadDecodeWidthPx), Dimension.Undefined))
            .diskCacheKey(item.url)
            .memoryCacheKey(item.url)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .preloadRequest()
            .build()
    }
}
