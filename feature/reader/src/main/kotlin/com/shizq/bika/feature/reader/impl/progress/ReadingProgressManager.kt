package com.shizq.bika.feature.reader.impl.progress

import com.shizq.bika.feature.reader.impl.layout.ReaderController
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

private val logger = KotlinLogging.logger("ProgressManager")

/**
 * 章节身份。用作 remember/LaunchedEffect 的**单一** key。
 *
 * 旧实现有三个 key 描述同一件事：manager 的 key 是 initialPage、dataSource 的 key 是
 * imageList、controller（在 rememberReaderContext 里）的 key 是 chapterOrder。
 * 切到一个 initialPage 恰好相同的章节时（自动衔接下一章 → 0，或任何没读过的章 → 0），
 * 前两个 key 不变，恢复和跟踪都不会重启，而 controller 已经换了新的——
 * 于是跟踪协程还在 collect 旧 controller 的 flow。
 */
data class ChapterKey(
    val comicId: String,
    val chapterOrder: Int,
)

/**
 * 协调恢复与跟踪。**由 ViewModel 持有**，不由 composition 持有。
 *
 * 职责边界：
 * - 本类只做「观察 + 决定要不要写」，不碰 Room
 * - 实际写入交给 [ReadingProgressWriter]（在 viewModelScope 里）
 * - 需要 Compose 对象（controller / dataSource）的部分由 composition 通过
 *   [session] 驱动，随组合生命周期取消——这是正确的，controller 本就与组合同生死
 *
 * 旧实现把整个 manager 放在 rememberCoroutineScope() 上，导致「必须比组合活得久的写入」
 * 和「必须随组合销毁的观察」共用一个 scope，于是需要 persistLastKnownPage 这种
 * 同步逃生口 + 一整段 KDoc 契约来补偿。
 */
class ReadingProgressManager(
    private val writer: ReadingProgressWriter,
    private val restoreStrategy: ProgressRestoreStrategy,
    private val config: ProgressConfig,
) : ProgressWriteCoordinator {

    /** 恢复结果，null 表示尚未完成。供 UI 展示「恢复中/恢复失败」。 */
    val restoreOutcome: StateFlow<RestoreOutcome?>
        field = MutableStateFlow<RestoreOutcome?>(null)

    /**
     * 一次章节会话：恢复 → 确认 → 开闸 → 跟踪。
     *
     * 必须从 composition 的 LaunchedEffect(chapterKey) 调用，且该 effect 的 key
     * 就是 [ChapterKey]。key 变化时本函数所在协程被取消，跟踪随之停止——
     * 不会像旧实现那样把跟踪协程 launch 到一个更长寿的 scope 里，导致新旧两个
     * collector 并存。
     *
     * 挂起直到被取消（跟踪是无限的）。
     */
    suspend fun session(
        key: ChapterKey,
        targetPage: Int,
        totalPagesProvider: () -> Int,
        chapterTitleProvider: () -> String,
        dataSource: PageDataSource,
        controller: ReaderController,
    ) {
        restoreOutcome.value = null

        val outcome = restoreStrategy.restore(targetPage, dataSource, controller, config)
        restoreOutcome.value = outcome

        when (outcome) {
            is RestoreOutcome.Confirmed -> {
                logger.debug { "恢复确认，开闸: chapter=${key.chapterOrder} page=${outcome.page}" }
                writer.openGate()
            }

            is RestoreOutcome.Unconfirmed -> {
                // 你选的策略：未确认不写库。这里直接 return，不进入跟踪。
                // 用户手动滚动会产生新的 visibleItemIndex，但那些位置同样不可信
                // （我们不知道用户是"主动翻到这"还是"恢复失败停在这"），
                // 因此本次会话彻底不写。下次进入章节重新尝试恢复。
                logger.warn {
                    "恢复未确认，本次会话不写库: chapter=${key.chapterOrder} " +
                            "target=${outcome.targetPage} 停在=${outcome.reachedPage} " +
                            "原因=${outcome.reason}"
                }
                return
            }
        }

        trackPageChanges(key, totalPagesProvider, chapterTitleProvider, controller)
    }

    private suspend fun trackPageChanges(
        key: ChapterKey,
        totalPagesProvider: () -> Int,
        chapterTitleProvider: () -> String,
        controller: ReaderController,
    ) {
        controller.visibleItemIndex
            .distinctUntilChanged()
            .collectLatest { page ->
                writer.submit(
                    ChapterProgress(
                        comicId = key.comicId,
                        chapterOrder = key.chapterOrder,
                        pageIndex = page,
                        totalPages = totalPagesProvider(),
                        chapterTitle = chapterTitleProvider(),
                    )
                )
            }
    }

    /** ON_STOP / 返回 / 组合销毁：请求立即落库。同步返回。 */
    fun flush() = writer.flush()

    /**
     * 切章：把旧章节的已确认进度落库，并关闭闸门等待新章节恢复。
     *
     * 两件事必须一起做，所以合成一个方法——旧实现里「存旧章」在状态机 handler 里，
     * 「新章恢复」在 composition 的 LaunchedEffect 里，两者之间没有顺序约束，
     * 且闸门（一次性门闩）在切章后仍是打开的。
     *
     * 只在旧章节进度可信时才写：闸门为 Open 意味着旧章节的恢复曾被确认过。
     */
    override fun onChapterSwitch(previous: ChapterProgress?) {
        if (previous != null && writer.gate == PersistGate.Open) {
            writer.storeImmediately(previous)
        } else if (previous != null) {
            logger.debug {
                "切章但旧章节恢复未确认，不写库: chapter=${previous.chapterOrder} " +
                        "page=${previous.pageIndex}"
            }
        }
        writer.closeGate()
        restoreOutcome.value = null
    }
}
