package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.shizq.bika.R
import com.shizq.bika.paging.ComicPage

@Composable
fun ComicPageItem(
    page: ComicPage,
    index: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val imageRequest = remember(page.url) {
        ImageRequest.Builder(context)
            .data(page.url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .size(Size.ORIGINAL)
            .build()
    }

    // 外层容器
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = "Page ${index + 1}",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        ) {
            val state = painter.state

            when (state) {
                is AsyncImagePainter.State.Loading, is AsyncImagePainter.State.Empty -> {
                    // === 加载中状态 ===
                    // 使用 Box 包裹，以便在占位图之上叠加文字
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center // 1. 关键：设定内容左侧垂直居中对齐
                    ) {
                        // A. 占位图 (铺满)
                        Image(
                            painter = painterResource(id = R.drawable.placeholder_transparent_low),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // B. 页码文字 (显示在左侧)
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.Gray
                        )
                    }
                }

                is AsyncImagePainter.State.Success -> {
                    SubcomposeAsyncImageContent()
                }

                is AsyncImagePainter.State.Error -> {
                }
            }
        }
    }
}