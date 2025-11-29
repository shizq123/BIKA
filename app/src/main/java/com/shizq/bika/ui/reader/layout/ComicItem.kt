package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.shizq.bika.paging.ComicPage

private const val MAX_RETRY_COUNT = 3

@Composable
fun ComicPageItem(
    page: ComicPage,
    index: Int,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(page.url)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
}