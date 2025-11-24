package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.model.ReaderAction
import com.shizq.bika.paging.ComicPage
import com.shizq.bika.ui.reader.gesture.GestureState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

interface ReaderLayout {
    @Composable
    fun Content(
        comicPages: LazyPagingItems<ComicPage>,
        modifier: Modifier,
        onCurrentPageChanged: (Int) -> Unit = {},
    )
}

@Composable
fun ReaderLayout(
    readerContext: ReaderContext,
    gestureState: GestureState,
    comicPages: LazyPagingItems<ComicPage>,
    toggleMenuVisibility: () -> Unit,
    onCurrentPageChanged: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(3.seconds)
        focusRequester.requestFocus()
    }
    key(readerContext.layout) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val density = LocalDensity.current
            val viewportHeightPx = with(density) { maxHeight.toPx() }
            val scrollDistance = viewportHeightPx * 0.8f

            readerContext.layout.Content(
                comicPages = comicPages,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (!readerContext.config.volumeKeyNavigation) return@onPreviewKeyEvent false

                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.VolumeDown -> {
                                    scope.launch { readerContext.controller.nextPage(scrollDistance) }
                                    true
                                }

                                Key.VolumeUp -> {
                                    scope.launch { readerContext.controller.prevPage(scrollDistance) }
                                    true
                                }

                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                    .focusable()
                    .pointerInput(gestureState, readerContext.layout) {
                        detectTapGestures { offset ->
                            val action = gestureState.calculateAction(offset, size)
                            when (action) {
                                ReaderAction.NextPage -> scope.launch {
                                    readerContext.controller.nextPage(scrollDistance)
                                }

                                ReaderAction.PrevPage -> scope.launch {
                                    readerContext.controller.prevPage(scrollDistance)
                                }

                                ReaderAction.ToggleMenu -> toggleMenuVisibility()
                                ReaderAction.None -> {}
                            }
                        }
                    },
                onCurrentPageChanged = onCurrentPageChanged,
            )
        }
    }
}