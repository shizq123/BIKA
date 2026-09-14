package com.shizq.bika.feature.reader.impl.progress

import com.shizq.bika.feature.reader.impl.layout.ReaderController
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger("ProgressRestore")

/**
 * 恢复策略：等前置条件成立，再滚一次，然后确认。
 *
 * 与旧的 RetryRestoreStrategy 的根本区别：
 *
 * 旧实现每 100ms 无条件重调 `scrollToPage`，最多 150 次。之所以要重试，是因为
 * `LazyListState.scrollToItem` 对尚未加载的 index 会静默滚到末尾，调用方无法
 * 得知是否生效，只能不停试。但重试的退出条件（itemCount > targetPage）在
 * enablePlaceholders=true 下恒真，所以它实际上第一轮就"成功"返回了——
 * 既没有真正等到数据，也没有确认视口位置。
 *
 * 这里改成三段，每段都有可观测的成功判据：
 * 1. 等目标页数据真实到位（peek != null），超时或越界即放弃，不降级成功
 * 2. 滚一次
 * 3. 等 visibleItemIndex 确认到达（容许 tolerance 的误差），超时即 Unconfirmed
 *
 * 注意这套机制与 rememberLazyListState(initialFirstVisibleItemIndex) /
 * rememberPagerState(initialPage) / initialApiPage 是互补而非重复：那三者负责
 * 「首帧就大致落在目标附近」，这里负责「数据到位后精确确认」。
 */
interface ProgressRestoreStrategy {
    suspend fun restore(
        targetPage: Int,
        dataSource: PageDataSource,
        controller: ReaderController,
        config: ProgressConfig,
    ): RestoreOutcome
}

class AwaitDataRestoreStrategy : ProgressRestoreStrategy {

    override suspend fun restore(
        targetPage: Int,
        dataSource: PageDataSource,
        controller: ReaderController,
        config: ProgressConfig,
    ): RestoreOutcome {
        // 第 0 页无需恢复：任何 viewer 的初始位置就是 0。
        if (targetPage <= 0) return RestoreOutcome.Confirmed(0)

        if (currentPageOrNull(controller.visibleItemIndex) == targetPage) {
            return RestoreOutcome.Confirmed(targetPage)
        }

        val loadResult = withTimeoutOrNull(config.dataWaitTimeout) {
            dataSource.awaitLoadedOrBounds(targetPage)
        }

        when (loadResult) {
            null -> {
                logger.warn { "目标页数据未在 ${config.dataWaitTimeout} 内到位: target=$targetPage" }
                return RestoreOutcome.Unconfirmed(
                    targetPage = targetPage,
                    reachedPage = currentPageOrNull(controller.visibleItemIndex),
                    reason = "目标页数据加载超时",
                )
            }

            is PageLoadResult.OutOfBounds -> {
                // 章节缩水（服务端删图/重排）或历史数据本就越界。等下去也不会到位，
                // 立刻返回，不占满 dataWaitTimeout。
                logger.warn {
                    "目标页越界，放弃恢复: target=$targetPage 章节实际=${loadResult.actualTotal} 页"
                }
                return RestoreOutcome.Unconfirmed(
                    targetPage = targetPage,
                    reachedPage = currentPageOrNull(controller.visibleItemIndex),
                    reason = "目标页超出章节范围（章节实际 ${loadResult.actualTotal} 页）",
                )
            }

            PageLoadResult.Loaded -> Unit // 继续往下滚动
        }

        controller.scrollToPage(targetPage)

        // 确认视口真的到了。条漫的 firstVisibleItemIndex 在图片高度未测完时
        // 可能差一两项，故容许 tolerance。
        val confirmed = withTimeoutOrNull(config.confirmTimeout) {
            controller.visibleItemIndex.first { page ->
                abs(page - targetPage) <= config.confirmTolerance
            }
        }

        return if (confirmed != null) {
            logger.debug { "恢复已确认: target=$targetPage actual=$confirmed" }
            RestoreOutcome.Confirmed(confirmed)
        } else {
            val reached = currentPageOrNull(controller.visibleItemIndex)
            logger.warn { "恢复未确认: target=$targetPage 停在=$reached" }
            RestoreOutcome.Unconfirmed(
                targetPage = targetPage,
                reachedPage = reached,
                reason = "滚动后未确认到达目标页",
            )
        }
    }

    /**
     * 取当前页码，取不到返回 null。
     *
     * PagerController.visibleItemIndex 带 filterNotNull（spreads 为空时不发射），
     * 所以这里必须套超时——不能无限等一个可能永远不发射的 flow。
     * 旧实现在 ON_STOP 里直接 `.first()` 就是这个问题。
     */
    private suspend fun currentPageOrNull(flow: Flow<Int>): Int? =
        withTimeoutOrNull(ImmediateProbeTimeout) { flow.first() }

    private companion object {
        /** 探测「当前页码」的即时超时：flow 有值就立刻返回，没有就别等。 */
        private val ImmediateProbeTimeout = 50.milliseconds
    }
}
