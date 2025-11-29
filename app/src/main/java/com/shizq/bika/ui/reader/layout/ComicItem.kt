package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.shizq.bika.paging.ComicPage

@Composable
fun ComicPageItem(
    page: ComicPage,
    index: Int,
    modifier: Modifier = Modifier
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(page.url)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentDescription = "Page ${index + 1}",
        contentScale = ContentScale.FillWidth,
        modifier = modifier.fillMaxWidth(),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .background(Color.Gray.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(80.dp))
            }
        },
        error = {
            val painter = this.painter

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .background(Color.LightGray)
                    .clickable {
                        painter.restart()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                    Text(
                        text = "加载失败\n点击重试",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    )
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
                page = ComicPage(id = "1", url = "http://fake.url"),
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
                        page = ComicPage(id = "$index", url = "http://fake.url"),
                        index = index
                    )
                }
            }
        }
    }
}