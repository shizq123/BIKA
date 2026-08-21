package com.shizq.bika.feature.reader.impl

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
import com.shizq.bika.core.data.paging.Chapter
import com.shizq.bika.feature.reader.impl.components.ChapterList
import com.shizq.bika.feature.reader.impl.layout.SideSheetLayout
import com.shizq.bika.feature.reader.impl.state.ChapterState
import com.shizq.bika.feature.reader.impl.state.ReaderAction
import com.shizq.bika.feature.reader.impl.state.ReaderSheet
import com.shizq.bika.feature.reader.impl.state.UiControlState

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
            onDismissRequest = { dispatch(ReaderAction.HideSheet) },
            closeButton = {
                IconButton(onClick = { dispatch(ReaderAction.HideSheet) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭目录")
                }
            },
        ) {
            ChapterList(
                chapters = chapterItems,
                currentChapterOrder = chapterState.order,
                onChapterClick = { newChapter -> dispatch(ReaderAction.JumpToChapter(newChapter)) },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
