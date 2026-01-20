package com.shizq.bika.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.runtime.NavKey
import com.shizq.bika.core.ui.TrackDisposableJank
import com.shizq.bika.navigation.NavigationState
import com.shizq.bika.navigation.RegisterNavKey
import com.shizq.bika.navigation.TOP_LEVEL_ROUTES
import com.shizq.bika.navigation.rememberNavigationState
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberAppState(
    startDestination: NavKey,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): AppState {
    val navigationState = rememberNavigationState(RegisterNavKey, TOP_LEVEL_ROUTES)

    NavigationTrackingSideEffect(navigationState)

    return remember(
        navigationState,
        coroutineScope,
    ) {
        AppState(
            navigationState = navigationState,
            coroutineScope = coroutineScope,
        )
    }
}

@Stable
class AppState(
    val navigationState: NavigationState,
    coroutineScope: CoroutineScope,
)

/**
 * Stores information about navigation events to be used with JankStats
 */
@Composable
private fun NavigationTrackingSideEffect(navigationState: NavigationState) {
    TrackDisposableJank(navigationState.currentKey) { metricsHolder ->
        metricsHolder.state?.putState("Navigation", navigationState.currentKey.toString())
        onDispose {}
    }
}