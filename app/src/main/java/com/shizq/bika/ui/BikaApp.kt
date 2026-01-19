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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.shizq.bika.navigation.Navigator
import com.shizq.bika.navigation.dashboardEntry
import com.shizq.bika.navigation.feedEntry
import com.shizq.bika.navigation.historyEntry
import com.shizq.bika.navigation.leaderboardEntry
import com.shizq.bika.navigation.loginEntry
import com.shizq.bika.navigation.readerNavKeyEntry
import com.shizq.bika.navigation.settingsEntry
import com.shizq.bika.navigation.toEntries
import com.shizq.bika.navigation.unitedDetailNavKeyEntry

@Composable
fun BikaApp(
    appState: AppState,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {

    val navigator = remember { Navigator(appState.navigationState) }

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
            val entryProvider = entryProvider {
                loginEntry(navigator)
                dashboardEntry(navigator)
                feedEntry(navigator)
                leaderboardEntry(navigator)
                unitedDetailNavKeyEntry(navigator)
                readerNavKeyEntry(navigator)
                historyEntry(navigator)
                settingsEntry(navigator)
            }

            NavDisplay(
                entries = appState.navigationState.toEntries(entryProvider),
                onBack = { navigator.goBack() },
            )
        }
    }
}