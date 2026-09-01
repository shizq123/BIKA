package com.shizq.bika.feature.reader.impl.layout

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.magnifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.oshai.kotlinlogging.KotlinLogging
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.core.ui.CircularProgressIndicator
import com.shizq.bika.core.ui.isRetryableError
import kotlinx.coroutines.delay
import me.saket.telephoto.zoomable.EnabledZoomGestures
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

private val pagingLogger = KotlinLogging.logger("ReaderPaging")
private val imageLogger = KotlinLogging.logger("ReaderImage")

/**
 * 分页数据未就绪时的占位组件：
 * - 加载中：显示进度条
 * - 分页失败：显示可点击的重试按钮，并持续退避重试（间隔 2s/4s/8s/16s/30s 封顶），
 *   网络恢复后无需手动操作即可重新获取数据；首次失败记录原因日志
 */
@Composable
fun ChapterPageLoadStateItem(
    pageItems: LazyPagingItems<ChapterPage>,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val refreshError = pageItems.loadState.refresh as? LoadState.Error
    val appendError = pageItems.loadState.append as? LoadState.Error
    val isError = refreshError != null || appendError != null

    var autoRetryCount by remember(pageItems) { mutableIntStateOf(0) }
    LaunchedEffect(isError, autoRetryCount) {
        if (isError) {
            val error = refreshError?.error ?: appendError?.error
            if (autoRetryCount == 0) {
                if (error.isRetryableError()) {
                    pagingLogger.error(error) { "章节分页加载失败: 第 ${index + 1} 页" }
                } else {
                    // 404 等永久失败：提示后不再自动重试
                    pagingLogger.warn(error) { "章节分页永久不可用(不重试): 第 ${index + 1} 页" }
                }
            }
            if (error.isRetryableError()) {
                val delayMs = (2000L shl autoRetryCount).coerceAtMost(30_000L)
                autoRetryCount++
                delay(delayMs)
                pageItems.retry()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .background(if (isError) Color.LightGray else Color.Gray.copy(alpha = 0.1f))
            .clickable(enabled = isError) { pageItems.retry() },
        contentAlignment = Alignment.Center
    ) {
        if (isError) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                Text(
                    text = "第 ${index + 1} 页加载失败\n点击重试",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
        }
    }
}

/**
 * 单页渲染。
 *
 * [zoomable] 为 true 时本页自己承接缩放与点击（翻页模式）：每页独立缩放，
 * 翻到下一页时缩放自动复位。条漫模式传 false，由容器整体缩放。
 *
 * [onTap] 收到的坐标是**根坐标系**下的位置。跨页模式一屏有两页，用页面局部
 * 坐标会把右页的左半边当成「屏幕左侧」，导致点击翻页方向反掉。
 */
@Composable
fun ComicPageItem(
    page: ChapterPage,
    index: Int,
    modifier: Modifier = Modifier,
    zoomable: Boolean = false,
    onTap: ((PageTapContext) -> Unit)? = null,
    onSizeLoaded: ((width: Float, height: Float) -> Unit)? = null
) {
    val config = LocalReaderConfig.current
    var magnifierCenter by remember { mutableStateOf(Offset.Unspecified) }

    // 缩放状态不需要按 page.id 做 key：翻页模式下 Pager 的 key 已经包含页码与
    // 图片 id，换页就是换节点，state 随节点一起重建，缩放不会残留到下一页。
    val zoomableState = rememberZoomableState(ZoomSpec(maxZoomFactor = 4f))
    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentOnTap by rememberUpdatedState(onTap)

    val zoomModifier = if (zoomable) {
        Modifier
            // onGloballyPositioned 必须在 zoomable **之前**：放在之后拿到的是
            // 已经过缩放变换的坐标系，换算出来的点击位置会随缩放倍数漂移。
            .onGloballyPositioned { rootCoordinates = it }
            .zoomable(
                state = zoomableState,
                gestures = EnabledZoomGestures.ZoomAndPan,
                onClick = { localOffset ->
                    val handler = currentOnTap ?: return@zoomable
                    val coords = rootCoordinates
                    if (coords == null || !coords.isAttached) return@zoomable
                    // 转到根坐标系，并取根节点尺寸作为视口尺寸
                    val root = coords.findRootCoordinates()
                    val rootOffset = root.localPositionOf(coords, localOffset)
                    handler(
                        PageTapContext(
                            position = rootOffset,
                            viewportSize = root.size,
                        )
                    )
                }
            )
    } else {
        Modifier
    }

    val magnifierModifier = if (config.magnifierEnabled) {
        Modifier
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        magnifierCenter = offset
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        magnifierCenter = if (magnifierCenter != Offset.Unspecified) {
                            magnifierCenter + dragAmount
                        } else {
                            magnifierCenter
                        }
                    },
                    onDragEnd = {
                        magnifierCenter = Offset.Unspecified
                    },
                    onDragCancel = {
                        magnifierCenter = Offset.Unspecified
                    }
                )
            }
            .magnifier(
                sourceCenter = { magnifierCenter },
                magnifierCenter = {
                    if (magnifierCenter != Offset.Unspecified) {
                        magnifierCenter - Offset(0f, 150f)
                    } else {
                        Offset.Unspecified
                    }
                },
                zoom = 1.8f
            )
    } else {
        Modifier
    }

    val configuration = LocalConfiguration.current
    val platformContext = LocalPlatformContext.current
    val contentScale = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        ContentScale.Fit
    } else {
        ContentScale.FillWidth
    }
    var imageAspectRatio by remember(page.id) { mutableFloatStateOf(0.75f) }
    val imageRequest = remember(platformContext, page.url) {
        ImageRequest.Builder(platformContext)
            .data(page.url)
            .crossfade(false)
            .diskCacheKey(page.url)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    val painter = rememberAsyncImagePainter(model = imageRequest)

    val state by painter.state.collectAsState()

    // 重试计数必须在 when 之外：声明在 Error 分支内时，state 一旦离开 Error
    // （例如重试后短暂进入 Loading）计数就被丢弃，退避永远从 2s 重新开始，
    // 持续失败的图片会变成固定 2s 一次的无限轮询。
    var imageRetryCount by remember(page.id) { mutableIntStateOf(0) }
    val errorState = state as? AsyncImagePainter.State.Error
    LaunchedEffect(errorState, imageRetryCount) {
        val error = errorState?.result?.throwable ?: return@LaunchedEffect
        if (imageRetryCount == 0) {
            if (error.isRetryableError()) {
                imageLogger.error(error) { "图片加载失败: 第 ${index + 1} 页 url=${page.url}" }
            } else {
                // 404 等永久失败：提示后不再自动重试，避免无效请求与日志刷屏
                imageLogger.warn(error) { "图片永久不可用(不重试): 第 ${index + 1} 页 url=${page.url}" }
            }
        }
        if (error.isRetryableError()) {
            // 用 coerceAtMost 前先限制位移量：shl 的右操作数按 mod 32 取模，
            // imageRetryCount 涨到 32 时 2000L shl 32 会绕回 2000，退避失效。
            val delayMs = (2000L shl imageRetryCount.coerceAtMost(4)).coerceAtMost(30_000L)
            imageRetryCount++
            delay(delayMs)
            painter.restart()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(imageAspectRatio)
            .animateContentSize(animationSpec = tween(durationMillis = 200))
            .then(zoomModifier)
            .then(magnifierModifier),
    ) {
        Image(
            painter = painter,
            contentDescription = "Page ${index + 1}",
            contentScale = contentScale,
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
                // 退避重试逻辑已提到 when 之外，这里只负责 UI
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray)
                        .clickable {
                            // 手动重试时重置计数，让用户的显式操作立即生效
                            imageRetryCount = 0
                            painter.restart()
                        },
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
                val intrinsicSize = state.painter?.intrinsicSize

                // 上报尺寸不能放在 `if (ratio != newRatio)` 里：跨页分组依赖这个
                // 回调判定宽页，而恰好等于当前 ratio 的页会被跳过，宽页永远测不出来。
                LaunchedEffect(intrinsicSize) {
                    if (intrinsicSize != null && intrinsicSize.width > 0 && intrinsicSize.height > 0) {
                        imageAspectRatio = intrinsicSize.width / intrinsicSize.height
                        onSizeLoaded?.invoke(intrinsicSize.width, intrinsicSize.height)
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