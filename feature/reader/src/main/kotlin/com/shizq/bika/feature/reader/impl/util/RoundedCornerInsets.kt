package com.shizq.bika.feature.reader.impl.util

import android.os.Build
import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun rememberTopEndRoundedCornerRadius(): Dp {
    val view = LocalView.current
    val density = LocalDensity.current

    var radiusPx by remember {
        mutableIntStateOf(0)
    }

    DisposableEffect(view) {
        fun updateRadius() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val insets = view.rootWindowInsets
                val roundedCorner = insets?.getRoundedCorner(
                    android.view.RoundedCorner.POSITION_TOP_RIGHT
                )

                radiusPx = roundedCorner?.radius ?: 0
            } else {
                radiusPx = 0
            }
        }

        updateRadius()

        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            updateRadius()
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        view.requestApplyInsets()

        onDispose {
            if (view.viewTreeObserver.isAlive) {
                view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        }
    }

    return with(density) {
        radiusPx.toDp()
    }
}
@Composable
fun rememberTopEndSystemAwarePadding(
    includeStatusBarInset: Boolean = false,
    extraTop: Dp = 0.dp,
    extraEnd: Dp = 0.dp,
): TopEndPadding {
    val layoutDirection = LocalLayoutDirection.current

    val roundedCornerRadius = rememberTopEndRoundedCornerRadius()

    val cornerOffset = roundedCornerRadius * 0.2929f

    val statusBarTop = if (includeStatusBarInset) {
        WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    } else {
        0.dp
    }

    val displayCutoutPaddingValues = WindowInsets.displayCutout.asPaddingValues()

    val cutoutEnd = displayCutoutPaddingValues.calculateEndPadding(layoutDirection)

    val top = maxOf(
        cornerOffset,
        statusBarTop,
    ) + extraTop

    val end = maxOf(
        cornerOffset,
        cutoutEnd
    ) + extraEnd

    return TopEndPadding(
        top = top,
        end = end
    )
}
@Stable
data class TopEndPadding(
    val top: Dp,
    val end: Dp
)