package com.shizq.bika.navigation

enum class TopLevelNavItem {
    Dashboard,
    Login,
    Register,
}

val TOP_LEVEL_NAV_ITEMS = mapOf(
    DashboardNavKey to TopLevelNavItem.Dashboard,
)