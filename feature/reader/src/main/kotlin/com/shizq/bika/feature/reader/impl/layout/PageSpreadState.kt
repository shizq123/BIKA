package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 跨页分组的共享状态，由 [PagerLayoutStrategy]（渲染）和 [PagerController]（页码换算）
 * 共同读取，保证两边看到的是同一份分组。
 *
 * 这个类现在只是 [SpreadLayout] 的 Compose 状态外壳：所有判定逻辑都在纯函数
 * [withMeasurement] / [withPageCount] 里，这里只负责把结果写进快照状态。
 * 分组与重定位请求来自同一次纯计算，不可能互相不一致。
 *
 * 重定位请求**不是**破坏性读取。之前的 `consumePendingAnchor()` 先清空再滚动，
 * 而滚动前还有一个「分组是否已就绪」的守卫——守卫为假时 anchor 已经没了，
 * 重定位永久丢失。而那个守卫恰好在最需要重定位的那一帧最可能为假。
 * 现在改成 [clearRelocation]，只在**确认到位后**由渲染层调用。
 */
@Stable
class PageSpreadState(
    doublePage: Boolean,
    private val pageCountProvider: () -> Int,
) {
    var layout: SpreadLayout by mutableStateOf(
        SpreadLayout.of(pageCount = pageCountProvider(), doublePage = doublePage),
    )
        private set

    /**
     * 待重定位的**真实页码**，null 表示无待处理请求。
     * 由 [PagerLayoutStrategy] 在滚动确认到位后调 [clearRelocation] 清除。
     */
    var relocateTo: Int? by mutableStateOf(null)
        private set

    val spreads: List<PageSpread> get() = layout.spreads

    /** Pager 的 pageCount，即翻页单位数量。 */
    val spreadCount: Int get() = layout.spreadCount

    val generation: Long get() = layout.generation

    /**
     * 同步分页续拉后的页数。
     *
     * 由渲染层在组合中调用（`itemCount` 是快照状态，变化会触发重组）。
     * 之前 spreads 是 `derivedStateOf { pageCountProvider() }`，页数变化自动生效；
     * 现在分组是普通状态，需要这一步显式推进。代价是多一个调用点，
     * 换来的是分组变化必须经由 [withMeasurement] / [withPageCount] 这两个纯函数。
     */
    fun syncPageCount() {
        val current = pageCountProvider()
        if (current != layout.pageCount) {
            layout = layout.withPageCount(current)
        }
    }

    /**
     * 图片解码后上报实际尺寸。
     *
     * @param anchorPage 上报时用户所在的真实页码，须来自不随本次重排变化的来源。
     */
    fun onPageMeasured(pageIndex: Int, width: Float, height: Float, anchorPage: Int? = null) {
        val result = layout.withMeasurement(pageIndex, width, height, anchorPage)
        if (result.layout === layout) return
        layout = result.layout
        // 已有未完成的重定位请求时不覆盖：先到的那个 anchor 才是用户真正的位置，
        // 后到的是在已经漂移过的分组上算出来的。
        if (result.relocateTo != null && relocateTo == null) {
            relocateTo = result.relocateTo
        }
    }

    /** 重定位已确认到位，清除请求。未到位时**不要**调用，否则请求会永久丢失。 */
    fun clearRelocation() {
        relocateTo = null
    }
}
