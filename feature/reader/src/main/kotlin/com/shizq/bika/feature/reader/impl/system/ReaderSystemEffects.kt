package com.shizq.bika.feature.reader.impl.system

import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.shizq.bika.core.context.findActivity
import com.shizq.bika.core.model.reader.ScreenOrientation
import com.shizq.bika.core.ui.composition.LocalWindow

/**
 * 阅读器所需的系统级副作用集合：屏幕方向锁定、常亮、系统栏显隐。
 */
@Composable
internal fun ReaderSystemEffects(
    showSystemBars: Boolean,
    screenOrientation: ScreenOrientation,
) {
    SystemBarsEffect(showSystemBars = showSystemBars)
    KeepScreenOnEffect()
    OrientationEffect(screenOrientation)
}

@Composable
private fun OrientationEffect(orientation: ScreenOrientation) {
    val context = LocalContext.current
    LaunchedEffect(orientation) {
        val activity = context.findActivity()
        activity?.requestedOrientation = when (orientation) {
            ScreenOrientation.System -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ScreenOrientation.Portrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            ScreenOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            ScreenOrientation.LockPortrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ScreenOrientation.LockLandscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            ScreenOrientation.ReversePortrait -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        }
    }
}

@Composable
private fun KeepScreenOnEffect() {
    val window = LocalWindow.current

    DisposableEffect(Unit) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
private fun SystemBarsEffect(showSystemBars: Boolean) {
    val window = LocalWindow.current

    DisposableEffect(window, showSystemBars) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (showSystemBars) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
