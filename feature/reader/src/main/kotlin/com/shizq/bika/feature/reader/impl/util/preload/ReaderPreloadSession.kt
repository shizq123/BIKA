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
 * 已经过期的窗口。当前窗口计算仍委托给 [ListPreloader]；后续迁移
 * Planner/Scheduler 时，只需要替换 [handle]，无需再次修改组合层生命周期装配。
 */
internal class ReaderPreloadSession<T : Any>(
    scope: CoroutineScope,
    dataProvider: PreloadDataProvider<T>,
    modelProvider: PreloadModelProvider<T>,
    enqueuer: PreloadRequestEnqueuer,
    closeEnqueuer: () -> Unit,
) {
    private sealed interface Event {
        data class ViewportChanged(
            val snapshot: ViewportSnapshot,
            val preloadCount: Int,
        ) : Event

        data object Close : Event
    }

    private val events = Channel<Event>(Channel.CONFLATED)
    private val preloader = ListPreloader(
        dataProvider = dataProvider,
        modelProvider = modelProvider,
        enqueuer = enqueuer,
        maxPreload = 0,
    )
    private val closeEnqueuer = closeEnqueuer
    private var closed = false

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
        events.trySend(Event.ViewportChanged(snapshot, preloadCount))
    }

    /** 兼容旧调用方：没有方向和原因时交由旧 preloader 处理。 */
    fun submitViewport(range: IntRange?, preloadCount: Int) {
        submitViewport(ViewportSnapshot(visibleRange = range), preloadCount)
    }

    private fun handle(event: Event.ViewportChanged) {
        preloader.maxPreload = event.preloadCount
        preloader.onViewport(event.snapshot)
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
