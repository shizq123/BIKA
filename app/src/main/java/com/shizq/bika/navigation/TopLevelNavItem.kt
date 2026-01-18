package com.shizq.bika.navigation

import androidx.navigation3.runtime.NavKey

enum class TopLevelNavItem {
    Dashboard,
    Login,
    Register,
}

val TOP_LEVEL_ROUTES: Set<NavKey> = setOf(
    DashboardNavKey,
    LoginNavKey,
//    RegisterNavKey
)