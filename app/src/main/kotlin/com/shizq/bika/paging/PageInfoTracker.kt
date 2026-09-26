package com.shizq.bika.paging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * 收敛「分页源旁路上报总页数」这件事，屏蔽过期数据源的回写。
 *
 * 需要这一层的原因是总页数的数据流向是反的：它不随 LoadResult 返回，而是由数据源在
 * Paging 的加载线程上回调写入。当排序或筛选变化导致 Pager 重建时，上一代数据源虽已被
 * 取消，但它的网络响应可能已经在途，回调仍会执行一次。若不加区分，UI 上的总页数会被
 * 上一次查询条件的结果覆盖，且这种错乱只在网络较慢时出现。
 *
 * 每次 [track] 发一个递增代号，回调只在自己仍是最新一代时才生效——最新的装配总是胜出。
 */
class PageInfoTracker {
    private val generation = AtomicInteger(0)
    private val state = MutableStateFlow(1)

    /** 当前总页数，最小为 1。首次加载完成前为 1。 */
    val totalPages: StateFlow<Int> = state.asStateFlow()

    /**
     * 接管 [source] 的总页数上报，并原样返回它，便于直接作为 Pager 工厂的产物。
     *
     * 形参上界要求 [PageInfoReporting]，因此新增分页源若忘记实现该接口会直接编译失败，
     * 而不是在运行期静默丢失总页数。返回值保留具体类型 [T]，调用方仍可把它当
     * `PagingSource` 使用。
     *
     * 必须在 Pager 开始加载前调用，即在 `Pager { }` 的工厂 lambda 内。
     */
    fun <T : PageInfoReporting> track(source: T): T {
        // 代号在装配时取快照，闭包捕获的是这一代的值
        val tracked = generation.incrementAndGet()
        source.onPageInfoLoaded = { pages, _ ->
            if (generation.get() == tracked) {
                // 结果集为空时服务端返回 0 页，会让 UI 渲染出「1 ~ 0」这种无法通过校验的区间
                state.value = pages.coerceAtLeast(1)
            }
        }
        return source
    }
}
