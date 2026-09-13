package com.shizq.bika.feature.reader.impl.progress

import com.shizq.bika.core.data.paging.ChapterMeta
import com.shizq.bika.feature.reader.impl.ReadingProgressStore

/**
 * 把 [ChapterProgress] 适配到既有的 [ReadingProgressStore]。
 *
 * 存在的意义是让 [ReadingProgressWriter] 的防抖/串行化逻辑可以在纯 JVM 单测里
 * 用假 sink 验证，不需要 Room。ReadingProgressStore 本身不动。
 */
class StoreBackedSink(
    private val store: ReadingProgressStore,
) : ChapterProgressSink {
    override suspend fun store(progress: ChapterProgress): Boolean =
        store.store(
            comicId = progress.comicId,
            chapterOrder = progress.chapterOrder,
            meta = ChapterMeta(
                title = progress.chapterTitle,
                totalImages = progress.totalPages,
            ),
            pageIndex = progress.pageIndex,
        )
}
