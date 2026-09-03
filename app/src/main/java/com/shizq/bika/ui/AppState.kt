package com.shizq.bika.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.shizq.bika.core.message.MessageSource
import com.shizq.bika.core.ui.TrackDisposableJank
import com.shizq.bika.navigation.Navigator
import com.shizq.bika.navigation.rememberNavigator
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberAppState(
    messageSource: MessageSource,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): AppState {
    val navigator = rememberNavigator()

    NavigationTrackingSideEffect(navigator)

    return remember(
        navigator,
        coroutineScope,
        messageSource,
    ) {
        AppState(
            navigator = navigator,
            coroutineScope = coroutineScope,
            messageSource = messageSource,
        )
    }
}

@Stable
class AppState(
    val navigator: Navigator,
    val coroutineScope: CoroutineScope,
    val messageSource: MessageSource,
)

/**
 * Stores information about navigation events to be used with JankStats.
 *
 * 跟踪主图栈顶而不是「当前处于哪个图」：后者只有两个取值，几乎不变化，作为
 * jank 归因的维度没有意义。
 */
@Composable
private fun NavigationTrackingSideEffect(navigator: Navigator) {
    val currentRoute = navigator.contentBackStack.lastOrNull()
    TrackDisposableJank(currentRoute) { metricsHolder ->
        metricsHolder.state?.putState(
            "Navigation",
            currentRoute.toString(),
        )
        onDispose {}
    }
}
