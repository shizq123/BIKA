package com.shizq.bika.feature.reader.impl.gesture

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

/**
 * 音量键翻页。
 *
 * 走 View 层的 OnUnhandledKeyEventListener 而不是 Modifier.onPreviewKeyEvent：
 * 后者要求组件持有焦点，阅读器内容区通常不可聚焦，注册了也收不到事件。
 *
 * 回调通过 [rememberUpdatedState] 保持最新。监听器只注册一次（key 是 view/enabled），
 * 导航请求本身由调用方统一调度，避免音量键监听器直接启动并发翻页协程。
 */
@Composable
fun VolumeKeyNavigation(
    enabled: Boolean,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
) {
    val view = LocalView.current
    val currentOnVolumeUp by rememberUpdatedState(onVolumeUp)
    val currentOnVolumeDown by rememberUpdatedState(onVolumeDown)

    DisposableEffect(view, enabled) {
        if (enabled) {
            val listener = ViewCompat.OnUnhandledKeyEventListenerCompat { _, event ->
                // repeatCount > 0 是长按产生的重复事件：系统按约 50ms 一次持续投递，
                // 全部响应会让长按音量键变成一次滚过十几页。只认第一次按下。
                if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) {
                    false
                } else {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_VOLUME_UP -> {
                            currentOnVolumeUp()
                            true
                        }

                        KeyEvent.KEYCODE_VOLUME_DOWN -> {
                            currentOnVolumeDown()
                            true
                        }

                        else -> false
                    }
                }
            }

            ViewCompat.addOnUnhandledKeyEventListener(view, listener)
            onDispose { ViewCompat.removeOnUnhandledKeyEventListener(view, listener) }
        } else {
            onDispose { }
        }
    }
}
