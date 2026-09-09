package com.shizq.bika.core.ui.message

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.shizq.bika.core.message.MessageDuration
import com.shizq.bika.core.message.MessageId
import com.shizq.bika.core.message.MessageOutcome
import com.shizq.bika.core.message.MessageSource
import com.shizq.bika.core.message.UserMessage
import com.shizq.bika.core.message.asString
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest

private fun MessageDuration.toSnackbarDuration(): SnackbarDuration = when (this) {
    MessageDuration.Short -> SnackbarDuration.Short
    MessageDuration.Long -> SnackbarDuration.Long
    MessageDuration.Indefinite -> SnackbarDuration.Indefinite
}

/**
 * 把 [MessageSource] 的消息接到 [SnackbarHostState] 上。
 *
 * 放在 Scaffold 内部调用一次即可。文案在这里才被解析成 String，
 * 因此上游可以全程只传 `UiText`。
 */
@Composable
fun UserMessageEffect(
    source: MessageSource,
    snackbarHostState: SnackbarHostState,
) {
    UserMessageEffect(
        current = source.current,
        snackbarHostState = snackbarHostState,
        onOutcome = source::onOutcome,
    )
}

/**
 * 不依赖 [MessageSource] 的版本，便于 Preview 和测试直接喂一个 StateFlow。
 */
@Composable
fun UserMessageEffect(
    current: StateFlow<UserMessage?>,
    snackbarHostState: SnackbarHostState,
    onOutcome: (MessageId, MessageOutcome) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(current, snackbarHostState) {
        // collectLatest：上游换消息时取消上一条的挂起展示，由 Snackbar 自己走退场动画。
        current.collectLatest { message ->
            if (message == null) return@collectLatest
            val result = snackbarHostState.showSnackbar(
                message = message.text.asString(context),
                actionLabel = message.action?.label?.asString(context),
                withDismissAction = message.duration == MessageDuration.Indefinite,
                duration = message.duration.toSnackbarDuration(),
            )
            val outcome = when (result) {
                SnackbarResult.ActionPerformed -> MessageOutcome.ActionPerformed
                SnackbarResult.Dismissed -> MessageOutcome.Dismissed
            }
            onOutcome(message.id, outcome)
        }
    }
}
