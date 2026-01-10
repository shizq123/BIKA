package com.shizq.bika.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.shizq.bika.ui.dashboard.DashboardScreen
import com.shizq.bika.ui.feed.FeedScreen
import com.shizq.bika.ui.feed.FeedViewModel

fun EntryProviderScope<NavKey>.dashboardEntry(navigator: Navigator) {
    entry<DashboardNavKey> {
        DashboardScreen(
            navigationToCollections = { navigator.navigate(FeedNavKey.Collection) },
            navigationToRandom = { navigator.navigate(FeedNavKey.Random) },
        )
    }
}

fun EntryProviderScope<NavKey>.feedEntry(navigator: Navigator) {
    entry<FeedNavKey.Collection> { key ->
        FeedScreen(
            onBackClick = { navigator.goBack() },
            viewModel = hiltViewModel<FeedViewModel, FeedViewModel.Factory>(
                key = key.toString(),
            ) { factory ->
                factory.create(key)
            },
        )
    }
    entry<FeedNavKey.Random> { key ->
        FeedScreen(
            onBackClick = { navigator.goBack() },
            viewModel = hiltViewModel<FeedViewModel, FeedViewModel.Factory>(
                key = key.toString(),
            ) { factory ->
                factory.create(key)
            },
        )
    }
}