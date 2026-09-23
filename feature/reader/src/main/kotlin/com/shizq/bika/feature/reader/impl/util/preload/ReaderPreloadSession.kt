package com.shizq.bika.feature.reader.impl.util.preload

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 一个章节阅读会话内的预载协调器。
 *
 * 预载相关的输入（视口、分页快照变化、预载策略变化）先进入事件队列，
 * 再由同一个协程串行处理。这样组合层不会直接修改预载器状态，切章时
 * 也可以通过 [close] 明确终止旧会话。
 *
 * 事件通道使用 CONFLATED：快速滚动时只保留最新视口，避免工作协程逐个处理
 * 已经过期的窗口。窗口规划、请求组装和 generation 切换均由会话串行完成，
 * 队列层只负责稳定 key 去重、并发执行和取消。
 */
internal class ReaderPreloadSession<T : Any>(
    scope: CoroutineScope,
    private val dataProvider: PreloadDataProvider<T>,
    private val modelProvider: PreloadModelProvider<T>,
    private val enqueuer: PreloadRequestEnqueuer,
    private val closeEnqueuer: () -> Unit,
) {
    private sealed interface Event {
        data class ViewportChanged(
            val snapshot: ViewportSnapshot,
            val preloadCount: Int,
        ) : Event

        data object Close : Event
    }

    private val events = Channel<Event>(Channel.CONFLATED)
    private val planner = PreloadPlanner()
    private var previousRequests = emptyMap<String, PreloadRequest>()
    private var closed = false
    private var latestGeneration = 0L
    private var highestSubmittedGeneration = 0L

    private val worker: Job = scope.launch {
        for (event in events) {
            when (event) {
                is Event.ViewportChanged -> handle(event)
                Event.Close -> break
            }
        }
    }

    fun submitViewport(snapshot: ViewportSnapshot, preloadCount: Int) {
        if (closed) return
        if (snapshot.generation < highestSubmittedGeneration) return
        highestSubmittedGeneration = snapshot.generation
        events.trySend(Event.ViewportChanged(snapshot, preloadCount))
    }

    /** 仅用于旧的范围型调用方；新的调用方应提交完整的 [ViewportSnapshot]。 */
    fun submitViewport(range: IntRange?, preloadCount: Int) {
        submitViewport(ViewportSnapshot(visibleRange = range), preloadCount)
    }

    private fun handle(event: Event.ViewportChanged) {
        val snapshot = event.snapshot
        val generation = snapshot.generation
        if (generation < latestGeneration) return
        if (generation > latestGeneration) {
            latestGeneration = generation
            resetWindow()
        }

        val visibleRange = snapshot.visibleRange
        if (visibleRange == null || event.preloadCount <= 0 || dataProvider.itemCount <= 0) {
            clearWindow()
            return
        }

        val plan = planner.plan(
            viewport = snapshot,
            preloadCount = event.preloadCount,
            itemCount = dataProvider.itemCount,
        )
        updateWindow(plan.indices, visibleRange)
    }

    private fun resetWindow() {
        planner.reset()
        clearWindow()
    }

    private fun clearWindow() {
        previousRequests = emptyMap()
        enqueuer.updateWindow(emptyList())
    }

    private fun updateWindow(indices: List<Int>, visibleRange: IntRange) {
        val visibleRequests = previousRequests.values
            .filter { it.index in visibleRange }
            .associateByTo(linkedMapOf(), PreloadRequest::key)
        val requests = linkedMapOf<String, PreloadRequest>()

        for (index in indices) {
            val item = dataProvider.getItem(index) ?: continue
            val request = modelProvider.getPreloadRequest(item) ?: continue
            val preload = PreloadRequest(
                index = index,
                key = modelProvider.getPreloadKey(item, request),
                request = request,
            )
            requests[preload.key] = preload
        }

        enqueuer.updateWindow(
            requests = requests.values.toList(),
            visibleRequests = visibleRequests.values.toList(),
        )
        previousRequests = visibleRequests + requests
    }

    suspend fun close() {
        if (closed) return
        closed = true
        withContext(NonCancellable) {
            events.trySend(Event.Close)
            worker.join()
            events.close()
            closeEnqueuer()
        }
    }
}
