package com.shizq.bika.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

fun EntryProviderScope<NavKey>.youEntry(navigator: Navigator) {
    entry<YouNavKey> {

    }
}