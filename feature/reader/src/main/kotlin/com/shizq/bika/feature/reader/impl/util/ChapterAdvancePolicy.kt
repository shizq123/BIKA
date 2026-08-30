package com.shizq.bika.feature.reader.impl.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 章节自动衔接策略：到达当前章节最后一页时，自动跳转到下一章。
 *
 * @property endOfChapterDebounce 页面停留在末尾多久后才判定为"确实看到了最后一页"，
 *   用于过滤快速划过末页又划回去的情况
 * @property advanceDelay 判定到达末尾后，延迟多久再触发跳转下一章，
 *   给末页一点视觉停留时间，避免体验上过于突兀
 */
data class ChapterAdvancePolicy(
    val endOfChapterDebounce: Duration = 800.milliseconds,
    val advanceDelay: Duration = 300.milliseconds,
) {
    /**
     * 判断给定页码是否已到达章节末页。
     *
     * @param pageIndex 当前页码（0-based）
     * @param totalPages 当前章节总页数；小于等于 0 表示总页数尚未加载完成，一律判定为未到达末页
     */
    fun isAtLastPage(pageIndex: Int, totalPages: Int): Boolean {
        if (totalPages <= 0) return false
        return pageIndex >= totalPages - 1
    }
}
