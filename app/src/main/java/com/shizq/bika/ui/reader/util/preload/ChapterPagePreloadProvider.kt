package com.shizq.bika.ui.reader.util.preload

import android.content.Context
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import com.shizq.bika.paging.ChapterPage

class ChapterPagePreloadProvider(private val context: Context) : PreloadModelProvider<ChapterPage> {
    override fun getPreloadRequest(item: ChapterPage): ImageRequest {
        return ImageRequest.Builder(context)
            .data(item.url)
            .size(Size.ORIGINAL)
            .diskCacheKey(item.url)
            .memoryCacheKey(item.url)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .allowHardware(true)
            .build()
    }
}