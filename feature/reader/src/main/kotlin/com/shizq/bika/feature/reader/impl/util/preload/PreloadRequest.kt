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

/** 预载请求的投递出口。 */
interface PreloadRequestEnqueuer {
    fun updateWindow(
        requests: List<PreloadRequest>,
        visibleRequests: List<PreloadRequest> = emptyList(),
    )
}

/**
 * 阅读会话内的预载任务。
 *
 * [index] 只用于判断任务是否进入可见区域；[key] 才是队列去重和保留任务的身份。
 */
data class PreloadRequest(
    val index: Int,
    val key: String,
    val request: ImageRequest,
)

internal class CoilPreloadRequestEnqueuer(
    context: Context,
    scope: CoroutineScope,
) : PreloadRequestEnqueuer {
    private val logger = KotlinLogging.logger {}
    private val imageLoader = context.imageLoader
    private val queue = PreloadQueue(
        scope = scope,
        keyOf = PreloadRequest::key,
        execute = { preload ->
            try {
                when (imageLoader.execute(preload.request)) {
                    is SuccessResult -> true
                    is ErrorResult -> false
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.warn(error) {
                    "预载失败，留给可见页请求重试"
                }
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
