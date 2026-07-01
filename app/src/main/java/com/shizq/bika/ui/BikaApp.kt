package com.shizq.bika.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.shizq.bika.navigation.Navigator
import com.shizq.bika.navigation.rootSection

val LocalUseBackAnimation = compositionLocalOf { false }

@Composable
fun BikaApp(
    appState: AppState,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2(),
    usePredictiveBack: Boolean = false,
) {

    val navigator = remember { Navigator(appState.navigationState) }

    val canGoBack by remember {
        derivedStateOf {
            if (appState.navigationState.currentRootDestination == com.shizq.bika.navigation.AuthenticationRoute) {
                appState.navigationState.authenticationBackStack.size > 1
            } else {
                val currentStack = appState.navigationState.backStacks[appState.navigationState.topLevelRoute]
                if (currentStack != null) {
                    val currentRoute = currentStack.lastOrNull()
                    val isAtBaseRoute = currentRoute == appState.navigationState.topLevelRoute
                    val isAtStartTab = appState.navigationState.topLevelRoute == appState.navigationState.topLevelStartRoute
                    !(isAtBaseRoute && isAtStartTab)
                } else {
                    false
                }
            }
        }
    }

    BackHandler(enabled = !usePredictiveBack && canGoBack) {
        navigator.goBack()
    }

    CompositionLocalProvider(LocalUseBackAnimation provides usePredictiveBack) {
        Scaffold(
            modifier = modifier.semantics {
                testTagsAsResourceId = true
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    ),
            ) {
                NavDisplay(
                    backStack = appState.navigationState.rootBackStack,
                    entryProvider = entryProvider {
                        rootSection(
                            navigator = navigator,
                            useAnimation = usePredictiveBack
                        )
                    },
                    onBack = navigator::goBack,
                )
            }
        }
    }
}