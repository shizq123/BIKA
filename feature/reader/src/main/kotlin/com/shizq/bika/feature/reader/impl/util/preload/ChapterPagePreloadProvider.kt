package com.shizq.bika.feature.reader.impl.util.preload

import android.content.Context
import coil3.annotation.ExperimentalCoilApi
import coil3.decode.BlackholeDecoder
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import com.shizq.bika.core.data.paging.ChapterPage

@OptIn(ExperimentalCoilApi::class)
class ChapterPagePreloadProvider(private val context: Context) : PreloadModelProvider<ChapterPage> {
    override fun getPreloadRequest(item: ChapterPage): ImageRequest {
        return ImageRequest.Builder(context)
            .data(item.url)
            .size(Size.ORIGINAL)
            .diskCacheKey(item.url)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.DISABLED)
            // Only warm disk cache. Decoding full comic pages here wastes memory and CPU;
            // the visible-page request will decode using its own rendering options.
            .decoderFactory(BlackholeDecoder.Factory())
            .build()
    }
}
