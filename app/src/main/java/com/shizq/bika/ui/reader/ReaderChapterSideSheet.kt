package com.shizq.bika.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.paging.Chapter
import com.shizq.bika.ui.reader.components.ChapterList
import com.shizq.bika.ui.reader.layout.SideSheetLayout
import com.shizq.bika.ui.reader.state.ChapterState
import com.shizq.bika.ui.reader.state.ReaderAction
import com.shizq.bika.ui.reader.state.ReaderAction.HideSheet
import com.shizq.bika.ui.reader.state.ReaderAction.JumpToChapter
import com.shizq.bika.ui.reader.state.ReaderSheet
import com.shizq.bika.ui.reader.state.UiControlState

@Composable
internal fun ReaderChapterSideSheet(
    chapterItems: LazyPagingItems<Chapter>,
    chapterState: ChapterState,
    uiControlState: UiControlState,
    dispatch: (ReaderAction) -> Unit,
) {
    val isVisible = uiControlState.readerSheet is ReaderSheet.ChapterList
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(animationSpec = tween(), initialOffsetX = { -it }),
        exit = slideOutHorizontally(animationSpec = tween(), targetOffsetX = { -it }),
    ) {
        SideSheetLayout(
            title = { Text("目录") },
            onDismissRequest = { dispatch(HideSheet) },
            closeButton = {
                IconButton(onClick = { dispatch(HideSheet) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭目录")
                }
            },
        ) {
            ChapterList(
                chapters = chapterItems,
                currentChapterOrder = chapterState.order,
                onChapterClick = { newChapter -> dispatch(JumpToChapter(newChapter)) },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
