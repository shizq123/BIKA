package com.shizq.bika.core.ui.composition

import android.view.Window
import androidx.compose.runtime.staticCompositionLocalOf

val LocalWindow =
    staticCompositionLocalOf<Window> { error("CompositionLocal LocalWindow not present") }
