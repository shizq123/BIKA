package com.shizq.bika.feature.reader.impl.progress

import com.shizq.bika.feature.reader.impl.progress.ProgressWriteCoordinator.Companion.NoOp


/**
 * 一次「读到哪了」的完整描述。
 *
 * 章节归属（[comicId] + [chapterOrder]）随页码一起携带，不在落库时才去读状态机 snapshot：
 * 旧实现的 `PersistProgress(page)` 只带页码，写哪一章取决于 action 被调度的时刻，
 * 切章瞬间的防抖写入会落到新章节名下。
 *
 * @property pageIndex 真实页 index（0-based）。跨页模式下由 PagerController 换算，
 *   这里永远是真实页码，不是翻页单位下标。
 * @property totalPages 章节总页数，0 表示 meta 尚未到达。落库时 pageCount=0 会让
 *   「是否读完」判定为 false，不会误标已看完。
 */
data class ChapterProgress(
    val comicId: String,
    val chapterOrder: Int,
    val pageIndex: Int,
    val totalPages: Int,
    val chapterTitle: String,
)

/**
 * 进度的最终落库口。抽成接口只为一件事：让 [ReadingProgressWriter] 的防抖/串行化
 * 逻辑能在不碰 Room 的情况下单测。生产实现是 ReadingProgressStore。
 */
fun interface ChapterProgressSink {
    suspend fun store(progress: ChapterProgress): Boolean
}

/**
 * 状态机需要的那一小块能力：切章时落旧章进度 + 关闸。
 *
 * 为什么是接口而不是直接注入 [ReadingProgressManager]：manager 需要 viewModelScope，
 * 在 Hilt 构造 ReaderStateMachine 时还不存在。状态机只依赖这一个方法，
 * 由 ViewModel 在 init 里把真实实现接上。默认 [NoOp] 保证永不为 null。
 */
interface ProgressWriteCoordinator {
    fun onChapterSwitch(previous: ChapterProgress?)

    companion object {
        val NoOp = object : ProgressWriteCoordinator {
            override fun onChapterSwitch(previous: ChapterProgress?) = Unit
        }
    }
}
