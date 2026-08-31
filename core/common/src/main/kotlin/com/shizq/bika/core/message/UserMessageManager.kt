package com.shizq.bika.core.message

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 单条展示 + FIFO 排队的消息中枢。
 *
 * 并发策略：所有队列变更都在 [lock] 内完成，但用户回调一律在锁外分发。
 * 回调里再次调用 [report] 或 [dismiss] 是合法的，不会自锁。
 */
@Singleton
class UserMessageManager @Inject constructor() : UserMessageMonitor {

    private val lock = Any()

    /** 队首即当前展示中的消息。 */
    private val queue = ArrayDeque<UserMessage>()

    private val _current = MutableStateFlow<UserMessage?>(null)
    override val current: StateFlow<UserMessage?> = _current.asStateFlow()

    override fun report(message: UserMessage): MessageId {
        synchronized(lock) {
            // 已在展示或排队中，视为重复上报直接丢弃。
            if (queue.any { it.id == message.id }) return message.id
            queue.addLast(message)
            if (queue.size == 1) _current.value = message
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
                _current.value = queue.firstOrNull()
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
            _current.value = queue.firstOrNull()
            removed
        }
        val action = finished.action ?: return
        when (outcome) {
            MessageOutcome.ActionPerformed -> action.onPerformed()
            MessageOutcome.Dismissed -> action.onDismissed?.invoke()
        }
    }

    /** 清空所有消息，不触发任何回调。用于登出等需要重置 UI 的场景。 */
    fun clear() {
        synchronized(lock) {
            queue.clear()
            _current.value = null
        }
    }
}
