package com.shizq.bika.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter

/**
 * 网络图片加载组件：
 * - 临时性失败最多自动重试 4 次，且总重试时间不超过 30 秒；
 * - 4xx、解码失败和未知终态进入最终失败；
 * - 只有最终失败状态才显示可点击的重试占位；
 * - Painter 只负责渲染，请求代数、重试和错误状态由 ImageRetryController 管理。
 */
@Composable
fun RetryableAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
) {
    val painter = rememberAsyncImagePainter(model = model)
    val controller = remember(painter) {
        ImageRetryController(painter)
    }
    val loadState by controller.state.collectAsState()
    var manualRetryNonce by remember(model) { mutableIntStateOf(0) }
    val modelKey = remember(model) {
        model?.toString() ?: "<null-model>"
    }

    LaunchedEffect(modelKey, manualRetryNonce) {
        controller.run(modelKey)
    }

    val isRetrying = loadState is ImageLoadState.Retrying
    val isFailed = loadState is ImageLoadState.Failed

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = contentScale,
            alignment = alignment,
            modifier = Modifier.fillMaxSize()
        )

        if (isRetrying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "正在重试…",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (isFailed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray.copy(alpha = 0.6f))
                    .clickable {
                        manualRetryNonce++
                    }
                    .semantics(mergeDescendants = true) {
                        this.contentDescription = "图片加载失败，点击重试"
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "加载失败\n点击重试",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
