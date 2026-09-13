package com.shizq.bika.feature.reader.impl.progress

/**
 * 恢复的结果。只有 [Confirmed] 允许放开写库闸门。
 *
 * 你选的策略是「恢复未确认成功就不允许写库」，因此这里刻意不提供
 * 旧实现里 `Timeout -> Restored(fallbackPage)` 那种降级成功的表示：
 * 降级意味着视口停在一个并非用户上次所在的位置，此时写库会用更小的页码
 * 覆盖真实进度。降级只影响「显示在哪」，不解锁「能不能写」。
 */
sealed interface RestoreOutcome {
    /** 视口已确认停在 [page]，可以开始跟踪并写库。 */
    data class Confirmed(val page: Int) : RestoreOutcome

    /**
     * 未能确认到达目标页。[reachedPage] 是视口实际停留的位置（可能为 null，表示未知），
     * 仅供 UI 展示；写库保持关闭。
     */
    data class Unconfirmed(
        val targetPage: Int,
        val reachedPage: Int?,
        val reason: String,
    ) : RestoreOutcome
}

/**
 * 写库闸门。跟踪协程只在 [PersistGate.Open] 下提交页码。
 *
 * 旧实现用 `state.first { Restored || RestoreFailed }` 做一次性门闩：
 * 它在恢复失败时同样放开，且只挡一次——放开后无法再关。
 */
enum class PersistGate { Closed, Open }
