package com.shizq.bika.ui.reader.layout

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.shizq.bika.paging.ChapterPage

@Composable
fun ComicPageItem(
    page: ChapterPage,
    index: Int,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scaleMode = if (isLandscape) ContentScale.Fit else ContentScale.FillWidth
    var imageAspectRatio by remember { mutableFloatStateOf(0.75f) }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(page.url)
            .crossfade(false)
            .diskCacheKey(page.url)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    )

    val state by painter.state.collectAsState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(imageAspectRatio)
            .animateContentSize(animationSpec = tween(durationMillis = 200)),
    ) {
        Image(
            painter = painter,
            contentDescription = "Page ${index + 1}",
            contentScale = scaleMode,
            modifier = Modifier.fillMaxSize()
        )
        when (state) {
            is AsyncImagePainter.State.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                }
            }

            is AsyncImagePainter.State.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray)
                        .clickable { painter.restart() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                        Text(
                            text = "加载失败\n点击重试",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            is AsyncImagePainter.State.Success -> {
                val intrinsicSize = (state as AsyncImagePainter.State.Success).painter.intrinsicSize

                LaunchedEffect(intrinsicSize) {
                    if (intrinsicSize.width > 0 && intrinsicSize.height > 0) {
                        val newRatio = intrinsicSize.width / intrinsicSize.height
                        if (imageAspectRatio != newRatio) {
                            imageAspectRatio = newRatio
                        }
                    }
                }
            }

            else -> {}
        }
    }
}

@Preview(
    name = "单个条目预览 (Light)",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
private fun PreviewComicPageItem() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("模拟加载中/失败状态：", modifier = Modifier.padding(bottom = 8.dp))
            ComicPageItem(
                page = ChapterPage(id = "1", url = "http://fake.url"),
                index = 4
            )
        }
    }
}

@Preview(
    name = "列表模拟预览",
    showSystemUi = true
)
@Composable
private fun PreviewComicList() {
    MaterialTheme {
        Surface {
            LazyColumn {
                item {
                    Text(
                        "漫画阅读器示例",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(3) { index ->
                    ComicPageItem(
                        page = ChapterPage(id = "$index", url = "http://fake.url"),
                        index = index
                    )
                }
            }
        }
    }
}