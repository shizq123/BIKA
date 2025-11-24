package com.shizq.bika.ui.reader.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.paging.ComicPage

interface ReaderLayout {
    @Composable
    fun Content(
        comicPages: LazyPagingItems<ComicPage>,
//        initialPage: Int,
        modifier: Modifier,
        onCurrentPageChanged: (Int) -> Unit = {},
    )
}