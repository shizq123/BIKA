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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter

/**
 * 网络不稳定的图片加载组件：
 * - 临时性失败（网络抖动/5xx）后自动退避重试（2s/4s/8s/16s/30s 封顶，无限次），
 *   网络恢复后自动重新获取；
 * - 永久性失败（404 等 4xx）不自动重试，显示可点击的重试占位；
 * - 首次失败记录原因日志，便于排查。
 *
 * 替代裸 [coil3.compose.AsyncImage]：后者加载失败后静默空白，没有任何重试入口。
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
    val state by painter.state.collectAsState()

    // 手动重试信号：递增即重启退避协程，重试计数归零，用户的显式操作立即生效
    // 而不用等当前退避走完。计数本身活在协程栈上（见 autoRetryOnError）。
    var manualRetryNonce by remember(model) { mutableIntStateOf(0) }
    // key 必须包含 model，不能只有 painter：rememberAsyncImagePainter 在 model
    // 变化时复用同一个 painter 实例、只改它的 request，painter 的标识是稳定的。
    // 只按 painter 做 key 时，被 Lazy 列表复用到新 model 上的节点会继承上一张图
    // 的退避计数与"已记日志"标记——新图第一次失败不记日志，且直接从 30s 起等。
    LaunchedEffect(model, manualRetryNonce) {
        painter.autoRetryOnError { model.toString() }
    }

    val isError = state is AsyncImagePainter.State.Error
    // 注意：不要在正常态挂 clickable(enabled = isError)。
    // enabled=false 的 clickable 仍会参与 hit test 并消费 down 事件，
    // 导致外层卡片（如漫画详情页的推荐卡片、阅读历史的 ComicCard）的
    // 点击被静默吞掉，表现为“点击无反应”。只有错误态才需要拦截点击。
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
        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray.copy(alpha = 0.6f))
                    .clickable {
                        manualRetryNonce++
                        painter.restart()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
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
