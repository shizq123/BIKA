package com.shizq.bika.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.shizq.bika.ui.dashboard.DashboardScreen
import com.shizq.bika.ui.feed.ComicFeedScreen

fun EntryProviderScope<NavKey>.dashboardEntry(navigator: Navigator) {
    entry<DashboardNavKey> {
//        navigator.navigate(ComicFeedNavKey.CollectionNavKey)
        DashboardScreen()
    }
}

fun EntryProviderScope<NavKey>.comicFeedEntry(navigator: Navigator) {
    entry<ComicFeedNavKey> {
        ComicFeedScreen()
    }
}