package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 当前阅读位置。
 *
 * 必须区分 [first] 与 [last]：跨页模式下一屏含两页，只用一个 Int 表达位置时
 * 「读到哪」和「读完没」这两个问题的答案会被迫共用一个值，而它们需要的是不同的端点。
 * - 进度保存取 [first]：用户「读到」的是这一屏的起始页。
 * - 末页判定取 [last]：末屏是 Double(n-2, n-1) 时 [first] 恒为 n-2，
 *   永远追不上 totalPages - 1，章节自动衔接与「已读完」都不会触发。
 *
 * 单页模式下两者相等。
 */
@Immutable
data class ReadingPositionSnapshot(
    val first: Int,
    val last: Int,
) {
    /** 进度保存用的页码。 */
    val forProgress: Int get() = first

    /** 末页判定用的页码。 */
    val forEndOfChapter: Int get() = last

    /** 当前屏覆盖的页码范围，预载按它取真实页码。 */
    val range: IntRange get() = first..last

    /** 该屏是否包含指定页码。 */
    fun contains(pageIndex: Int): Boolean = pageIndex in first..last

    companion object {
        fun single(pageIndex: Int) = ReadingPositionSnapshot(pageIndex, pageIndex)
    }
}

/**
 * 阅读器控制接口
 * 屏蔽了底层实现（LazyColumn vs Pager）的差异
 */
interface ReaderController {
    /**
     * 当前阅读位置，**唯一真相**，由 Compose 快照状态支撑，可同步读。
     *
     * 之前这里是 `visibleItemIndex: Flow<Int>`，且是冷的 `snapshotFlow`。它被至少
     * 四处独立 collect（页码徽章/进度条、章节自动衔接、进度跟踪、恢复确认），
     * 每个 collector 各跑一遍位置计算，而条漫的计算里还带一个「沿用上次有效值」
     * 的可变字段——N 个 collector 并发写同一个字段。
     *
     * 位置是一个可读的值，不是一串事件。需要流语义的消费者用 [positionFlow]。
     */
    val position: ReadingPositionSnapshot

    /**
     * [position] 的**唯一写入点**，由装配处（`rememberReaderContext`）启动一次。
     *
     * 挂起直到被取消。放在接口上而不是留给调用方自己 launch，是因为漏调用的表现
     * 是「页码永远停在初始值」——不崩不报错，只有用户发现进度不动。
     */
    suspend fun track()

    /**
     * 像素级连续滚动能力，null 表示该 viewer 不支持（如 Pager）。
     * 调用方（自动滚动）应据此决定是否展示入口，而不是调用一个可能静默失效的方法。
     */
    val continuousScroller: ContinuousScroller?

    /**
     * 翻到下一页
     * - 条漫模式：向下滚动一屏（通常是高度的 80%）
     * - 翻页模式：切换到下一个**翻页单位**（跨页模式下即前进两页，这是正确语义：
     *   逐页前进会让画面变成 1|2 → 2|3）
     */
    suspend fun scrollNextPage()

    /**
     * 翻到上一页
     */
    suspend fun scrollPrevPage()

    /**
     * 跳转到指定页码（用于目录跳转或进度条拖动）
     */
    suspend fun scrollToPage(index: Int)
}

/**
 * 给非 Compose 消费者（进度跟踪、恢复策略）的流视图。
 *
 * 做成扩展而不是接口成员：接口上同时放 `position` 和 `positionFlow` 会变成两个
 * 必须保持同步的成员，正是本次要消除的问题。这里只有一个来源。
 */
fun ReaderController.positionFlow(): Flow<ReadingPositionSnapshot> =
    snapshotFlow { position }.distinctUntilChanged()
