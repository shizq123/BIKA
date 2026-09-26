package com.shizq.bika.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.shizq.bika.core.message.MessageSource
import com.shizq.bika.navigation.Navigator
import com.shizq.bika.navigation.rememberNavigator
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberAppState(
    messageSource: MessageSource,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): AppState {
    val navigator = rememberNavigator()

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
