package com.shizq.bika.feature.reader.impl.gesture

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import kotlinx.coroutines.launch

/**
 * 音量键翻页。
 *
 * 走 View 层的 OnUnhandledKeyEventListener 而不是 Modifier.onPreviewKeyEvent：
 * 后者要求组件持有焦点，阅读器内容区通常不可聚焦，注册了也收不到事件。
 *
 * 回调在内部用 [rememberUpdatedState] 包裹并由内部 scope 调度：
 * 监听器只注册一次（key 是 view/enabled），若直接捕获回调会锁死首次组合时的
 * 那个闭包，正确性不该押在调用方记得自己包一层。
 */
@Composable
fun VolumeKeyNavigation(
    enabled: Boolean,
    onVolumeUp: suspend () -> Unit,
    onVolumeDown: suspend () -> Unit,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val currentOnVolumeUp by rememberUpdatedState(onVolumeUp)
    val currentOnVolumeDown by rememberUpdatedState(onVolumeDown)

    DisposableEffect(view, enabled) {
        if (enabled) {
            val listener = ViewCompat.OnUnhandledKeyEventListenerCompat { _, event ->
                if (event.action != KeyEvent.ACTION_DOWN) {
                   false
                }else{
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_VOLUME_UP -> {
                            scope.launch { currentOnVolumeUp() }
                            true
                        }

                        KeyEvent.KEYCODE_VOLUME_DOWN -> {
                            scope.launch { currentOnVolumeDown() }
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
