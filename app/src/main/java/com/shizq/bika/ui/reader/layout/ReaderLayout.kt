package com.shizq.bika.ui.reader.layout

import android.view.KeyEvent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
        pageItems: LazyPagingItems<ChapterPage>,
        modifier: Modifier,
    )
}

@Composable
fun ReaderLayoutHost(
    readerContext: ReaderContext,
    gestureState: GestureState,
    pageItems: LazyPagingItems<ChapterPage>,
    toggleMenuVisibility: () -> Unit,
    onHideMenu: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val zoomableState = rememberZoomableState(ZoomSpec(maxZoomFactor = 4f))
    val currentReaderContext by rememberUpdatedState(readerContext)
    val currentGestureState by rememberUpdatedState(gestureState)
    val currentOnHideMenu by rememberUpdatedState(onHideMenu)
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {

                    // 计算滑动的总距离 (包含 x 轴和 y 轴)
                    // getDistance() 等于 sqrt(x*x + y*y)
                    val distance = available.getDistance()

                    if (distance > 10f) {
                        currentOnHideMenu()
                    }
                }
                return Offset.Zero
            }
        }
    }
    VolumeButtonsHandler(
        enable = readerContext.config.volumeKeyNavigation,
        onVolumeUp = {
            scope.launch { currentOnHideMenu(); currentReaderContext.controller.scrollPrevPage() }
        },
        onVolumeDown = {
            scope.launch { currentOnHideMenu(); currentReaderContext.controller.scrollNextPage() }
        }
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewSize = IntSize(constraints.maxWidth, constraints.maxHeight)
        val layout = readerContext.layout
        key(layout::class) {
            layout.Content(
                pageItems = pageItems,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                    .zoomable(
                        state = zoomableState,
                        gestures = EnabledZoomGestures.ZoomAndPan,
                        onClick = { offset ->
                            val action = currentGestureState.calculateAction(offset, viewSize)
                            when (action) {
                                ReaderAction.NextPage -> scope.launch {
                                    currentOnHideMenu()
                                    currentReaderContext.controller.scrollNextPage()
                                }

                                ReaderAction.PrevPage -> scope.launch {
                                    currentOnHideMenu()
                                    currentReaderContext.controller.scrollPrevPage()
                                }

                                ReaderAction.ToggleMenu -> toggleMenuVisibility()
                                ReaderAction.None -> Unit
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