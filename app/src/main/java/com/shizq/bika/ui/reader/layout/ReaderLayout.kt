package com.shizq.bika.ui.reader.layout

import android.view.KeyEvent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.core.view.ViewCompat
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.model.ReaderAction
import com.shizq.bika.paging.ChapterPage
import com.shizq.bika.ui.reader.gesture.GestureState
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.EnabledZoomGestures
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

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
    val zoomableState = rememberZoomableState(ZoomSpec(maxZoomFactor = 4f, minZoomFactor = .5f))
    val currentReaderContext by rememberUpdatedState(readerContext)
    val currentGestureState by rememberUpdatedState(gestureState)
    val currentToggleMenuVisibility by rememberUpdatedState(toggleMenuVisibility)

    VolumeButtonsHandler(
        enable = readerContext.config.volumeKeyNavigation,
        onVolumeUp = {
            scope.launch { currentReaderContext.controller.scrollPrevPage() }
        },
        onVolumeDown = {
            scope.launch { currentReaderContext.controller.scrollNextPage() }
        }
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewSize = IntSize(constraints.maxWidth, constraints.maxHeight)

        key(readerContext.config.readingMode) {
            readerContext.layout.Content(
                chapterPages = chapterPages,
                modifier = Modifier
                    .fillMaxSize()
                    .zoomable(
                        state = zoomableState,
                        gestures = EnabledZoomGestures.ZoomAndPan,
                        onClick = { offset ->
                            val action = currentGestureState.calculateAction(offset, viewSize)
                            when (action) {
                                ReaderAction.NextPage -> scope.launch {
                                    currentReaderContext.controller.scrollNextPage()
                                }

                                ReaderAction.PrevPage -> scope.launch {
                                    currentReaderContext.controller.scrollPrevPage()
                                }

                                ReaderAction.ToggleMenu -> currentToggleMenuVisibility()
                                ReaderAction.None -> { /* Do nothing */
                                }
                            }
                        }
                    )
            )
        }
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