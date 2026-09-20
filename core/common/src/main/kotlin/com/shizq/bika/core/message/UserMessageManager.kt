package com.shizq.bika.core.message

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 单条展示 + FIFO 排队的消息中枢。
 *
 * 并发策略：队列变更与 [current] 的写入都在 [lock] 内完成，
 * 两者必须原子，否则并发的 report/dismiss 交错后 [current] 可能指向已出队的消息。
 * 只有用户回调在锁外分发，因此回调里再次调用 [report] 或 [dismiss] 不会自锁。
 */
@Singleton
internal class UserMessageManager @Inject constructor() : UserMessageMonitor {

    private val lock = Any()

    /** 队首即当前展示中的消息。 */
    private val queue = ArrayDeque<UserMessage>()

    override val current: StateFlow<UserMessage?>
        field = MutableStateFlow<UserMessage?>(null)

    override fun report(message: UserMessage): MessageId {
        synchronized(lock) {
            // 已在展示或排队中，视为重复上报直接丢弃。
            if (queue.any { it.id == message.id }) return message.id
            val wasEmpty = queue.isEmpty()
            queue.addLast(message)
            if (wasEmpty) syncCurrent()
        }
        return message.id
    }

    override fun dismiss(id: MessageId) {
        val dismissedHead = synchronized(lock) {
            val index = queue.indexOfFirst { it.id == id }
            if (index < 0) return
            val wasHead = index == 0
            val removed = queue.removeAt(index)
            if (wasHead) {
                syncCurrent()
                removed
            } else {
                null
            }
        }
        // 只有正在展示的消息被关闭才算 dismissed；还在排队时被撤销不触发回调。
        dismissedHead?.action?.onDismissed?.invoke()
    }

    override fun onOutcome(id: MessageId, outcome: MessageOutcome) {
        val finished = synchronized(lock) {
            // 忽略过期回传：UI 侧的退场动画可能晚于一次主动 dismiss。
            if (queue.firstOrNull()?.id != id) return
            val removed = queue.removeFirst()
            syncCurrent()
            removed
        }
        val action = finished.action ?: return
        when (outcome) {
            MessageOutcome.ActionPerformed -> action.onPerformed()
            MessageOutcome.Dismissed -> action.onDismissed?.invoke()
        }
    }

    override fun clear() {
        synchronized(lock) {
            queue.clear()
            syncCurrent()
        }
    }

    /** 把队首同步给 [current]。调用方必须持有 [lock]。 */
    private fun syncCurrent() {
        current.value = queue.firstOrNull()
    }
}
