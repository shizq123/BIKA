package com.shizq.bika.core.message

import java.util.UUID

/**
 * 消息标识。用 value class 包装避免与 [UserMessage.text] 之类的 String 参数混淆，
 * 运行时无装箱开销。
 */
@JvmInline
value class MessageId(val value: String) {
    companion object {
        fun random(): MessageId = MessageId(UUID.randomUUID().toString())
    }
}

enum class MessageSeverity { Info, Warning, Error }

enum class MessageDuration { Short, Long, Indefinite }

/**
 * 消息上的操作按钮。label 与回调绑定在一起，
 * 从类型上排除「有按钮无回调」和「有回调无按钮」这两种非法状态。
 */
class MessageAction(
    val label: UiText,
    val onPerformed: () -> Unit,
    /**
     * 消息未经点击而结束时触发：展示超时、用户手动划走、
     * 或被 [MessageReporter.dismiss] 主动关闭。
     */
    val onDismissed: (() -> Unit)? = null,
)

/**
 * 一条面向用户的提示消息。
 *
 * 相等性与哈希仅由 [id] 决定。原因有两点：
 * 一是回调是引用相等的，参与比较会让 `StateFlow` 的合流去重永久失效；
 * 二是这样传入稳定的 [id] 即可获得去重语义，同一 id 的消息重复上报时会被合并。
 */
class UserMessage(
    val text: UiText,
    val severity: MessageSeverity = MessageSeverity.Error,
    val duration: MessageDuration = MessageDuration.Short,
    val action: MessageAction? = null,
    val id: MessageId = MessageId.random(),
) {
    init {
        require(duration != MessageDuration.Indefinite || action != null) {
            "Indefinite 消息必须提供 action，否则用户无法关闭它"
        }
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is UserMessage && other.id == id)

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "UserMessage(id=${id.value}, severity=$severity, " +
                "duration=$duration, hasAction=${action != null})"
}
