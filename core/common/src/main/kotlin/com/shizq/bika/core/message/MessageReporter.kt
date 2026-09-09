package com.shizq.bika.core.message

import kotlinx.coroutines.flow.StateFlow

/**
 * 消息生产端。业务层只依赖这个接口，拿不到消费消息的能力。
 */
interface MessageReporter {
    /**
     * 上报一条消息。若同 [UserMessage.id] 的消息已在展示或排队中，则忽略本次上报。
     *
     * @return 该消息的 id，可用于后续 [dismiss]。
     */
    fun report(message: UserMessage): MessageId

    /** 主动关闭指定消息。若它正在展示，会触发 [MessageAction.onDismissed]。 */
    fun dismiss(id: MessageId)
}

/** 消息如何结束的。由 UI 层在消息消失时回传给 [MessageSource.onOutcome]。 */
enum class MessageOutcome {
    /** 用户点击了操作按钮。 */
    ActionPerformed,

    /** 超时、划走或被主动关闭。 */
    Dismissed,
}

/**
 * 消息消费端。UI 层只依赖这个接口，避免在 Composable 里误发消息。
 */
interface MessageSource {
    /** 当前应展示的消息，null 表示无。 */
    val current: StateFlow<UserMessage?>

    /**
     * 通知消息已结束，队列随之推进。回调的分发也在这里完成，
     * 因此 UI 层无需自己调用 [MessageAction.onPerformed]。
     */
    fun onOutcome(id: MessageId, outcome: MessageOutcome)
}

/** 读写合体，仅供 DI 装配与测试使用，业务代码请注入更窄的那一侧。 */
interface UserMessageMonitor : MessageReporter, MessageSource

fun MessageReporter.reportError(
    text: UiText,
    duration: MessageDuration = MessageDuration.Short,
    action: MessageAction? = null,
    id: MessageId = MessageId.random(),
): MessageId = report(UserMessage(text, MessageSeverity.Error, duration, action, id))

fun MessageReporter.reportWarning(
    text: UiText,
    duration: MessageDuration = MessageDuration.Short,
    action: MessageAction? = null,
    id: MessageId = MessageId.random(),
): MessageId = report(UserMessage(text, MessageSeverity.Warning, duration, action, id))

fun MessageReporter.reportInfo(
    text: UiText,
    duration: MessageDuration = MessageDuration.Short,
    action: MessageAction? = null,
    id: MessageId = MessageId.random(),
): MessageId = report(UserMessage(text, MessageSeverity.Info, duration, action, id))
