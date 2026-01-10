package com.shizq.bika.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.shizq.bika.ui.dashboard.DashboardScreen

fun EntryProviderScope<NavKey>.dashboardEntry(navigator: Navigator) {
    entry<DashboardNavKey> {
        DashboardScreen()
    }
}