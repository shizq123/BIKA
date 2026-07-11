package com.shizq.bika.core.network.image

import coil3.ImageLoader
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 基于注入的 Coil [ImageLoader] 的 [ImageCacheManager] 实现，不依赖全局单例。 */
@Singleton
internal class CoilImageCacheManager @Inject constructor(
    private val imageLoader: ImageLoader,
) : ImageCacheManager {

    override suspend fun diskCacheSize(): Long = withContext(Dispatchers.IO) {
        imageLoader.diskCache?.size ?: 0L
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            imageLoader.diskCache?.clear()
            imageLoader.memoryCache?.clear()
        }
    }
}
