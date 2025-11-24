package com.shizq.bika.core.coroutine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update

/**
 * @see Flow.restartable
 */
class FlowRestarter {
    internal val id = MutableStateFlow(0)

    /**
     * 重启 flow. 这将会导致 flow 从上游最开始的地方重新开始, 例如执行所有的 `map`, `filter` 等操作.
     */
    fun restart() {
        id.update { it + 1 }
    }
}

/**
 * 使 flow 可以被重启. 等价于:
 *
 * ```
 * private val id = MutableStateFlow(0) // FlowRestarter.restart 相当于 id.value++
 *
 * val result = id.flatMapLatest { _ ->
 *    upstream.map { ... }...
 *    // 重启时, 会重新执行上面的 map, filter 等操作
 * }
 * ```
 */
fun <T> Flow<T>.restartable(restarter: FlowRestarter): Flow<T> = restarter.id.flatMapLatest { this }

