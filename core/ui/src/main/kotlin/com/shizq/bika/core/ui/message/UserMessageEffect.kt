package com.shizq.bika.core.ui.message

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.shizq.bika.core.message.MessageDuration
import com.shizq.bika.core.message.MessageId
import com.shizq.bika.core.message.MessageOutcome
import com.shizq.bika.core.message.MessageSeverity
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
 * 消息宿主：把 [MessageSource] 接到 Snackbar 上，并按 [MessageSeverity] 着色。
 *
 * 着色放在这里而不是让调用方自己传 Snackbar 内容，是因为 severity 只有
 * [MessageSource] 知道，而 [SnackbarData] 不携带它——Material 的 SnackbarHost
 * 只传递 message/actionLabel。用 `current` 的 severity 给正在展示的那条上色：
 * 队列是单条展示的（见 UserMessageManager），所以队首就是屏幕上那条。
 */
@Composable
fun UserMessageSnackbarHost(
    source: MessageSource,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // collectAsState 而非 collectAsStateWithLifecycle：core:ui 未依赖
    // lifecycle-runtime-compose，且这里只在 Snackbar 可见时读取，
    // 不存在后台持续收集的浪费。
    val current by source.current.collectAsState()

    UserMessageEffect(source = source, snackbarHostState = snackbarHostState)

    SnackbarHost(snackbarHostState, modifier = modifier) { data ->
        val severity = current?.severity ?: MessageSeverity.Info
        Snackbar(
            snackbarData = data,
            containerColor = severity.containerColor(),
            contentColor = severity.contentColor(),
            actionColor = severity.contentColor(),
        )
    }
}

@Composable
private fun MessageSeverity.containerColor(): Color = when (this) {
    // Info 沿用 Material 默认的 inverseSurface，避免普通提示过于抢眼
    MessageSeverity.Info -> MaterialTheme.colorScheme.inverseSurface
    MessageSeverity.Warning -> MaterialTheme.colorScheme.tertiaryContainer
    MessageSeverity.Error -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun MessageSeverity.contentColor(): Color = when (this) {
    MessageSeverity.Info -> MaterialTheme.colorScheme.inverseOnSurface
    MessageSeverity.Warning -> MaterialTheme.colorScheme.onTertiaryContainer
    MessageSeverity.Error -> MaterialTheme.colorScheme.onErrorContainer
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
