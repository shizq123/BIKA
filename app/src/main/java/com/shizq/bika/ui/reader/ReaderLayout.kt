package com.shizq.bika.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.paging.ComicPage
import com.shizq.bika.ui.reader.layout.ReaderContext

@Composable
fun ReaderLayout(
    readerContext: ReaderContext,
    comicPages: LazyPagingItems<ComicPage>,
    currentPage: Int,
    onCurrentPageChanged: (Int) -> Unit
) {
    key(readerContext.layout) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            readerContext.layout.Content(
                comicPages = comicPages,
                modifier = Modifier.fillMaxSize(),
                onCurrentPageChanged = onCurrentPageChanged,
//                initialPage = currentPage
            )
        }
    }
}
