package com.shizq.bika.feature.reader.impl.util.preload

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps only the current reading window, in nearest-page-first order.
 * All calls and [scope] must use the same single-threaded dispatcher (the UI dispatcher).
 * [execute] 返回 true 表示预载成功；返回 false 表示失败。失败任务在当前窗口
 * 内不会忙重试，离开窗口后清除失败标记，重新进入时可以再次尝试。

 */
internal class PreloadQueue<K : Any, T : Any>(
    private val scope: CoroutineScope,
    private val maxConcurrent: Int = 2,
    private val keyOf: (T) -> K,
    private val execute: suspend (T) -> Boolean,
) {
    init {
        require(maxConcurrent > 0)
    }

    private var wanted = linkedMapOf<K, T>()
    private val running = mutableMapOf<K, Job>()
    private val completed = mutableSetOf<K>()
    private val failedInWindow = mutableSetOf<K>()
    private var closed = false

    fun update(items: List<T>, retainRunning: Set<K> = emptySet()) {
        if (closed) return
        wanted = items.associateByTo(linkedMapOf(), keyOf)
        completed.retainAll(wanted.keys)
        failedInWindow.retainAll(wanted.keys)

        // A prefetched page that has just become visible should finish its download.
        // Its foreground request can then read the same disk entry without restarting.
        running.filterKeys { it !in wanted && it !in retainRunning }.values.forEach { it.cancel() }
        drain()
    }

    fun close() {
        closed = true
        wanted.clear()
        completed.clear()
        failedInWindow.clear()
        running.values.toList().forEach { it.cancel() }
    }

    private fun drain() {
        if (closed || !scope.isActive) return
        // Snapshot the window: a synchronous cache hit may complete during job.start().
        for ((key, item) in wanted.toList()) {
            if (running.size >= maxConcurrent) break
            if (key in running || key in completed || key in failedInWindow) continue
            var succeeded = false
            val job = scope.launch(start = CoroutineStart.LAZY) {
                succeeded = execute(item)
            }
            running[key] = job
            // A rapid swipe can cancel a dispatched job before its body ever starts.
            // Completion handlers still run in that case, unlike a finally in the body.
            job.invokeOnCompletion { cause ->
                running.remove(key)
                if (cause == null && key in wanted) {
                    if (succeeded) {
                        completed.add(key)
                    } else {
                        // Do not spin on a persistent failure while the same window stays active.
                        // Leaving the window clears this marker, so returning later can retry.
                        failedInWindow.add(key)
                    }
                }
                drain()
            }
            job.start()
        }
    }
}
