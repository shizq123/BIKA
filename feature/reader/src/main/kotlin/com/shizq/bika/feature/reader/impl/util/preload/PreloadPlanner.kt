package com.shizq.bika.feature.reader.impl.util.preload

/**
 * 纯窗口规划结果。indices 已按执行优先级排序，越靠前越应优先预载。
 */
internal data class PreloadPlan(
    val indices: List<Int>,
    val direction: ScrollDirection,
)

/**
 * 阅读会话内的纯窗口规划器。
 *
 * 它只保存最近一次可信的阅读方向，不保存 ImageRequest，也不把列表下标当成资源身份。
 * 数据刷新和布局重排只刷新当前计划，不改变阅读方向；程序跳转采用目标两侧的保守窗口。
 */
internal class PreloadPlanner {
    private var direction: ScrollDirection = ScrollDirection.Forward

    fun plan(
        viewport: ViewportSnapshot,
        preloadCount: Int,
        itemCount: Int,
    ): PreloadPlan {
        val range = viewport.visibleRange
        if (range == null || preloadCount <= 0 || itemCount <= 0) {
            return PreloadPlan(emptyList(), direction)
        }

        if (viewport.cause == ViewportChangeCause.UserScroll && viewport.direction != null) {
            direction = viewport.direction
        }

        val indices = when (viewport.cause) {
            ViewportChangeCause.ProgrammaticJump -> aroundTarget(
                visibleRange = range,
                count = preloadCount,
                itemCount = itemCount,
            )

            ViewportChangeCause.UserScroll,
            ViewportChangeCause.LayoutReflow,
            ViewportChangeCause.DataRefresh,
            ViewportChangeCause.Unknown -> directional(
                visibleRange = range,
                count = preloadCount,
                itemCount = itemCount,
                direction = direction,
            )
        }
        return PreloadPlan(indices, direction)
    }

    private fun directional(
        visibleRange: IntRange,
        count: Int,
        itemCount: Int,
        direction: ScrollDirection,
    ): List<Int> {
        val start = when (direction) {
            ScrollDirection.Forward -> visibleRange.last + 1
            ScrollDirection.Backward -> visibleRange.first - 1
        }
        val step = if (direction == ScrollDirection.Forward) 1 else -1
        return buildList(count) {
            repeat(count) { offset ->
                val index = start + offset * step
                if (index in 0 until itemCount) add(index)
            }
        }
    }

    /**
     * 跳转后没有可靠阅读方向。先取目标后方一页，再取前方一页，交替向外扩展，
     * 在恢复进度、拖动进度条和直接定位时兼顾两个方向。
     */
    private fun aroundTarget(
        visibleRange: IntRange,
        count: Int,
        itemCount: Int,
    ): List<Int> = buildList(count) {
        var distance = 1
        while (size < count) {
            var added = false
            val forward = visibleRange.last + distance
            if (forward in 0 until itemCount) {
                add(forward)
                added = true
                if (size == count) break
            }

            val backward = visibleRange.first - distance
            if (backward in 0 until itemCount) {
                add(backward)
                added = true
            }

            if (!added) break
            distance++
        }
    }
}
