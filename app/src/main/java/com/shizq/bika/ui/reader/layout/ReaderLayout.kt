package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.model.ReaderAction
import com.shizq.bika.paging.ComicPage
import com.shizq.bika.ui.reader.gesture.GestureState
import kotlinx.coroutines.launch

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
    key(readerContext.layout) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val density = LocalDensity.current
            val screenHeight = with(density) { maxHeight.toPx() * 0.8f }

            readerContext.layout.Content(
                comicPages = comicPages,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(gestureState, readerContext.layout) {
                        detectTapGestures { offset ->
                            val action = gestureState.calculateAction(offset, size)
                            when (action) {
                                ReaderAction.NextPage -> scope.launch {
                                    readerContext.controller.nextPage(screenHeight)
                                }

                                ReaderAction.PrevPage -> scope.launch {
                                    readerContext.controller.prevPage(screenHeight)
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