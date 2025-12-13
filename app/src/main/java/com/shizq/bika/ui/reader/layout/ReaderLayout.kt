package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.model.ReaderAction
import com.shizq.bika.paging.ChapterPage
import com.shizq.bika.ui.reader.gesture.GestureState
import com.shizq.bika.ui.reader.gesture.volumeKeyHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

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
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(3.seconds)
        focusRequester.requestFocus()
    }

    key(readerContext.layout) {
        readerContext.layout.Content(
            chapterPages = chapterPages,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .volumeKeyHandler(
                    enabled = readerContext.config.volumeKeyNavigation,
                    scope = scope,
                    onVolumeUp = { readerContext.controller.scrollPrevPage() },
                    onVolumeDown = { readerContext.controller.scrollNextPage() }
                )
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