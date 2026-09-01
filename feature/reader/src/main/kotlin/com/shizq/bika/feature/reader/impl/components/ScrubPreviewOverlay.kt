package com.shizq.bika.feature.reader.impl.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.feature.reader.impl.util.ScrubState
import kotlinx.coroutines.delay

/** 拖动中命中未加载页时，停留超过这个时长才触发真实加载，避免快速划过时刷屏式请求。 */
private const val ScrubPreviewLoadDebounceMillis = 200L

/**
 * 拖动预览浮层：读取 [ScrubState.previewPageIndex]，独立成一个非 inline 的 Composable，
 * 使拖动过程中的重组只发生在这里，不波及调用方整个函数体。
 *
 * 取图优先用 [LazyPagingItems.peek]：它只读已加载缓存，不会把 index 记为“已访问”。
 * pageItems 同时被布局层（取 itemCount）、预载逻辑、实际渲染层
 * （WebtoonLayout/PagerLayout）共享，而 `get()`（即 `pageItems[index]`）会
 * 把 index 写入 Paging 内部的访问记录，供分页刷新锚点计算和 Paging 自身的
 * prefetch 使用。拖动时 onScrub 每帧调用，手指划过的每个索引都调 get() 等于
 * 每帧一次“污染访问记录 + 可能触发网络请求”，而其中绝大多数页用户根本不会
 * 真的停留。因此只在同一页停留超过 [ScrubPreviewLoadDebounceMillis] 后才退到
 * get()，此时用户大概率真的要去这一页，触发一次真实加载是合理的。
 */
@Composable
internal fun ScrubPreviewOverlay(
    scrubState: ScrubState,
    pageItems: LazyPagingItems<ChapterPage>,
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    val pageIndex = scrubState.previewPageIndex ?: return
    val cachedUrl = pageItems.peek(pageIndex)?.url

    var settledUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pageIndex, cachedUrl) {
        settledUrl = null
        if (cachedUrl == null) {
            delay(ScrubPreviewLoadDebounceMillis)
            settledUrl = pageIndex.takeIf { it in 0 until pageItems.itemCount }
                ?.let { pageItems[it]?.url }
        }
    }

    ScrubPreviewCard(
        pageUrl = cachedUrl ?: settledUrl,
        previewPageIndex = pageIndex,
        totalPages = totalPages,
        modifier = modifier,
    )
}

/**
 * @param previewPageIndex 拖动进度条时预览的目标页（0-based），不代表当前实际停留的页
 */
@Composable
private fun ScrubPreviewCard(
    pageUrl: String?,
    previewPageIndex: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.75f),
        border = BorderStroke(
            0.5.dp,
            Color.White.copy(alpha = 0.15f)
        ),
        shadowElevation = 4.dp,
        modifier = modifier
            .width(90.dp)
            .height(130.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!pageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(pageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Preview Page ${previewPageIndex + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${previewPageIndex + 1} / $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
