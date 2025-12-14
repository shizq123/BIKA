package com.shizq.bika.ui.reader.layout

import android.view.KeyEvent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.model.ReaderAction
import com.shizq.bika.paging.ChapterPage
import com.shizq.bika.ui.reader.gesture.GestureState
import kotlinx.coroutines.launch

interface ReaderLayout {
    @Composable
    fun Content(
        chapterPages: LazyPagingItems<ChapterPage>,
        modifier: Modifier,
    )
}

@Composable
fun ReaderLayout(
    readerContext: ReaderContext,
    gestureState: GestureState,
    chapterPages: LazyPagingItems<ChapterPage>,
    toggleMenuVisibility: () -> Unit
) {
    val scope = rememberCoroutineScope()

    VolumeButtonsHandler(
        enable = readerContext.config.volumeKeyNavigation,
        onVolumeUp = {
            scope.launch { readerContext.controller.scrollPrevPage() }
        },
        onVolumeDown = {
            scope.launch { readerContext.controller.scrollNextPage() }
        }
    )

    key(readerContext.layout) {
        readerContext.layout.Content(
            chapterPages = chapterPages,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(gestureState, readerContext.layout) {
                    detectTapGestures { offset ->
                        val action = gestureState.calculateAction(offset, size)
                        when (action) {
                            ReaderAction.NextPage -> scope.launch {
                                readerContext.controller.scrollNextPage()
                            }

                            ReaderAction.PrevPage -> scope.launch {
                                readerContext.controller.scrollPrevPage()
                            }

                            ReaderAction.ToggleMenu -> toggleMenuVisibility()
                            ReaderAction.None -> {}
                        }
                    }
                },
        )
    }
}

@Composable
fun VolumeButtonsHandler(
    enable: Boolean = true,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit
) {
    if (!enable) return

    val view = LocalView.current

    DisposableEffect(view) {
        val keyEventDispatcher = ViewCompat.OnUnhandledKeyEventListenerCompat { _, event ->
            if (event.action != KeyEvent.ACTION_DOWN) {
                return@OnUnhandledKeyEventListenerCompat false
            }

            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    onVolumeUp()
                    true
                }

                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    onVolumeDown()
                    true
                }

                else -> false
            }
        }

        ViewCompat.addOnUnhandledKeyEventListener(view, keyEventDispatcher)

        onDispose {
            ViewCompat.removeOnUnhandledKeyEventListener(view, keyEventDispatcher)
        }
    }
}