package com.shizq.bika.core.network.image

/**
 * 图片缓存的读量与清理入口。
 *
 * 屏蔽底层图片加载库（Coil）及其单例，使调用方无需触碰全局单例即可管理缓存。
 */
interface ImageCacheManager {
    /** 当前磁盘缓存占用字节数，无磁盘缓存时返回 0。 */
    suspend fun diskCacheSize(): Long

    /** 清空内存与磁盘缓存。 */
    suspend fun clear()
}
