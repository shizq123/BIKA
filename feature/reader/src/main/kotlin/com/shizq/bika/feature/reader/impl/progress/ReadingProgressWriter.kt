package com.shizq.bika.feature.reader.impl.progress

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration

private val logger = KotlinLogging.logger("ProgressWriter")

/**
 * 进度写入流水线：唯一的写入者。
 *
 * 旧实现有四个互不排序的写入者（防抖 job、ON_STOP 的 persistNow、onDispose 的
 * persistLastKnownPage、JumpToChapter handler 内的直接 store），共享三个可变字段
 * （persistJob / persistProgress / @Volatile lastKnownPage），且 persistJob 被
 * 生命周期回调协程和跟踪协程同时读写，没有互斥。
 *
 * 这里折叠成一条流：
 * - [submit] 提交「用户现在读到这」，走防抖
 * - [flush] 提交「立刻落库」的信号，不带数据，用最近一次 submit 的值
 *
 * 两路 merge 进单个 collector，写入顺序天然串行。所有可变字段消失。
 *
 * scope 必须是 viewModelScope 级别（比 composition 长寿）：旧实现用
 * rememberCoroutineScope()，组合销毁会掐死尚未触发的防抖 job，这正是
 * persistLastKnownPage 那个同步逃生口存在的唯一理由。
 */
class ReadingProgressWriter(
    private val sink: ChapterProgressSink,
    scope: CoroutineScope,
    private val debounce: Duration,
) {
    /**
     * replay = 1：flush 需要读「最近一次提交的值」。
     * extraBufferCapacity 给足，配合 tryEmit 让 submit 可以从非挂起上下文调用。
     */
    private val submissions = MutableSharedFlow<ChapterProgress>(
        replay = 1,
        extraBufferCapacity = 16,
    )
    private val flushSignals = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    /**
     * 绕过闸门与防抖的直接写入（切章）。仍然走同一个 collector，
     * 因此与防抖写入之间有确定的先后顺序——这是把切章写入「并进」流水线的关键：
     * 若用 scope.launch 直接调 sink，它与 collector 是两个并发写入者，
     * 切章瞬间可能被一条迟到的防抖写入覆盖。
     */
    private val immediateWrites = MutableSharedFlow<ChapterProgress>(extraBufferCapacity = 8)

    /** 写入闸门。恢复未确认前保持 Closed，[submit] 直接丢弃。 */
    @Volatile
    var gate: PersistGate = PersistGate.Closed
        private set

    init {
        merge(
            // 闸门在**发射时**再查一次，不能只在 submit 时查：
            // debounce 把待发值存在自己内部，closeGate() 无法撤回它。
            // 时序：submit(第 21 页) -> 用户切章 -> closeGate() -> 1 秒后防抖触发。
            // 若不在此处过滤，这条属于旧章节、且发生在切章写入之后的迟到写入
            // 会覆盖掉 storeImmediately 刚写好的正确值。
            submissions.debounce(debounce).filter { gate == PersistGate.Open },
            // flush 不携带数据：取 replayCache 里最近一次提交值，避免调用方
            // 需要自己记住「当前页是多少」（旧实现的 lastKnownPage 就是干这个的）。
            flushSignals.map { submissions.replayCache.lastOrNull() }.filterNotNull(),
            immediateWrites,
        )
            // 只对相邻重复去重。注意不能把 immediateWrites 排除在外——切章写入的
            // chapterOrder 与当前章不同，天然不会被去重掉。
            .distinctUntilChanged()
            .onEach { progress ->
                val ok = sink.store(progress)
                if (!ok) {
                    logger.warn { "进度落库失败: $progress" }
                }
            }
            .launchIn(scope)
    }

    /** 恢复确认后放开闸门。 */
    fun openGate() {
        if (gate != PersistGate.Open) {
            logger.debug { "写库闸门打开" }
            gate = PersistGate.Open
        }
    }

    /**
     * 提交一次页码变化，走防抖。闸门关闭时丢弃——你选的策略是
     * 「恢复未确认成功就不允许写库」，丢弃而非缓存，避免闸门打开瞬间
     * 把恢复期间的中间位置补写进去。
     */
    fun submit(progress: ChapterProgress) {
        if (gate != PersistGate.Open) {
            logger.debug { "闸门关闭，丢弃提交: page=${progress.pageIndex}" }
            return
        }
        submissions.tryEmit(progress)
    }

    /**
     * 请求立即落库（ON_STOP、返回、组合销毁）。
     *
     * 同步返回、不挂起：调用方可能是 onDispose 这类同步回调。实际写库在
     * [scope]（viewModelScope）里完成，不受组合生命周期影响。
     */
    fun flush() {
        if (gate != PersistGate.Open) return
        flushSignals.tryEmit(Unit)
    }

    /**
     * 切章前的强制落库：绕过闸门与防抖，直接提交指定进度。
     *
     * 切章是唯一需要绕过闸门的场景——旧章节的进度是已确认的（用户确实读到那），
     * 与新章节的恢复状态无关。JumpToChapter 走这条路，与其余三条路径共用
     * 同一个 sink，因此与防抖写入之间有明确顺序。
     */
    fun storeImmediately(progress: ChapterProgress) {
        immediateWrites.tryEmit(progress)
    }

    /**
     * 切章时重置：新章节的恢复尚未确认，闸门必须关回去。
     *
     * 旧实现的门闩（`state.first { Restored || RestoreFailed }`）是一次性的，
     * 放开后无法关闭，切章后新章节在恢复期间的中间位置会被直接写库。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun closeGate() {
        gate = PersistGate.Closed
        // 清掉上一章的 replayCache，避免 flush 把旧章节的页码写到新章节名下。
        submissions.resetReplayCache()
    }
}
