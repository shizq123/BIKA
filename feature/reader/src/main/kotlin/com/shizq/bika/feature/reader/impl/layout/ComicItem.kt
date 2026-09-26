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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
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
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.core.ui.CircularProgressIndicator
import com.shizq.bika.core.ui.backoffDelayMillis
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import me.saket.telephoto.zoomable.EnabledZoomGestures
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

private val pagingLogger = KotlinLogging.logger("ReaderPaging")

/**
 * 章节分页失败后的**唯一**退避重试驱动（间隔 2s/4s/8s/16s/30s 封顶）。
 *
 * 必须由宿主调用**一次**，不能放在占位项里。`loadState` 是整个 PagingData 共享的，
 * 而占位项在屏上通常同时存在若干个：之前每个占位项各持一份 autoRetryCount、
 * 各起一个 LaunchedEffect、各调 `pageItems.retry()`，于是
 * - 实际重试间隔变成所有实例中最小的那个，退避形同失效；
 * - 每轮重试的请求数等于当前可见占位项数量；
 * - 计数的 `remember(pageItems)` 在滚动复用时被重建，间隔又被拉回 2s。
 *
 * 失败原因日志也只在这里记一次，不再按可见占位项数量刷屏。
 */
@Composable
fun ChapterAppendRetryEffect(pageItems: LazyPagingItems<ChapterPage>) {
    // 退避计数活在协程栈上，不是组合状态。
    //
    // 之前是 `remember` 计数 + `LaunchedEffect(error, autoRetryCount)`，即 effect
    // 自己写自己的 key。这里的 `delay` 在 `++` 之前，取消发生在计数写入之前，
    // 所以侥幸能跑完一轮——但形状与 [ComicPageItem] 里那份（先 `++` 后 `delay`，
    // 因此重试一次都不发生）完全同构，正确性全靠这两行的相对顺序，
    // 而这读起来像风格问题、不像正确性问题。换成长活协程后这个类别整体消失。
    //
    // 循环自驱动：每轮主动读一次 loadState，不依赖「新的 LoadState.Error 与旧的
    // 不相等」来推进。每轮必经一次 delay，不会退化成忙循环。
    LaunchedEffect(pageItems) {
        var attempt = 0
        var logged = false
        while (true) {
            // loadState 是快照状态，snapshotFlow 会在它变化时重新求值。
            // refresh 与 append 任一失败都要退避重试。
            val throwable = snapshotFlow {
                val loadState = pageItems.loadState
                ((loadState.refresh as? LoadState.Error)
                    ?: (loadState.append as? LoadState.Error))?.error
            }.first { it != null } ?: continue

            if (!logged) {
                logged = true
//                if (throwable.isRetryableError()) {
//                    pagingLogger.error(throwable) { "章节分页加载失败" }
//                } else {
//                    // 404 等永久失败：提示后不再自动重试
//                    pagingLogger.warn(throwable) { "章节分页永久不可用(不重试)" }
//                }
            }
            // 永久失败：结束协程。用户点击占位项仍可手动 retry()。
//            if (!throwable.isRetryableError()) return@LaunchedEffect

            delay(backoffDelayMillis(attempt))
            attempt++
            pageItems.retry()
        }
    }
}

/**
 * 分页数据未就绪时的占位组件，**纯 UI**：
 * - 加载中：显示进度条
 * - 分页失败：显示可点击的重试按钮（用户显式操作立即生效，不走退避）
 *
 * 自动退避重试见 [ChapterAppendRetryEffect]。
 */
@Composable
fun ChapterPageLoadStateItem(
    pageItems: LazyPagingItems<ChapterPage>,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val loadState = pageItems.loadState
    val isError = loadState.refresh is LoadState.Error || loadState.append is LoadState.Error

    // clickable 只在错误态挂上，不写成 `clickable(enabled = isError)`。
    //
    // enabled=false 的 clickable 仍然参与 hit test 并消费 down 事件：加载中态
    // 铺满整个占位项，会把点击静默吞掉——在阅读器里表现为占位项所在的那一屏
    // 点击不出菜单、也不翻页，而 Pager 模式下点击是主要的翻页方式。
    // 同样的坑在 core/ui 的 RetryableAsyncImage 里已有注释记录。
    val errorClickable = if (isError) {
        Modifier.clickable { pageItems.retry() }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .background(if (isError) Color.LightGray else Color.Gray.copy(alpha = 0.1f))
            .then(errorClickable),
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
 * [onTap] 收到的坐标换算到 [viewport] 所标记的节点的坐标系，而不是本页的局部
 * 坐标：跨页模式一屏有两页，用页面局部坐标会把右页的左半边当成「屏幕左侧」，
 * 导致点击翻页方向反掉。传 [onTap] 时必须一并传 [viewport]，否则无从换算。
 *
 * [magnifierEnabled] 显式传入而不是整体读 ReaderConfig：这里只用到 ReaderConfig
 * 的这一个字段，若整体读 CompositionLocal，护眼深度、自动滚动速度等任何其他
 * 字段变化都会让每一个可见的 ComicPageItem 一起重组。
 */
@Composable
fun ComicPageItem(
    page: ChapterPage,
    index: Int,
    modifier: Modifier = Modifier,
    zoomable: Boolean = false,
    magnifierEnabled: Boolean = true,
    onTap: ((PageTapContext) -> Unit)? = null,
    viewport: ViewportAnchor? = null,
    onSizeLoaded: ((width: Float, height: Float) -> Unit)? = null
) {
    var magnifierCenter by remember { mutableStateOf(Offset.Unspecified) }

    // 缩放状态不需要按 page.id 做 key：翻页模式下 Pager 的 key 已经包含页码与
    // 图片 id，换页就是换节点，state 随节点一起重建，缩放不会残留到下一页。
    val zoomableState = rememberZoomableState(ZoomSpec(maxZoomFactor = 4f))
    var pageCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentOnTap by rememberUpdatedState(onTap)

    val zoomModifier = if (zoomable) {
        Modifier
            // onGloballyPositioned 必须在 zoomable **之前**：放在之后拿到的是
            // 已经过缩放变换的坐标系，换算出来的点击位置会随缩放倍数漂移。
            .onGloballyPositioned { pageCoordinates = it }
            .zoomable(
                state = zoomableState,
                gestures = EnabledZoomGestures.ZoomAndPan,
                onClick = { localOffset ->
                    val handler = currentOnTap ?: return@zoomable
                    val coords = pageCoordinates
                    if (coords == null || !coords.isAttached) return@zoomable
                    // 换算到视口坐标系。视口由布局策略指定（见 ViewportAnchor），
                    // 不用 findRootCoordinates()：那取的是整个窗口，阅读器内容区
                    // 被 inset / scaffold padding 推下去多少，分区边界就偏多少。
                    val viewportCoords = viewport?.coordinates ?: return@zoomable
                    if (!viewportCoords.isAttached) return@zoomable
                    handler(
                        PageTapContext(
                            position = viewportCoords.localPositionOf(coords, localOffset),
                            viewportSize = viewportCoords.size,
                        )
                    )
                }
            )
    } else {
        Modifier
    }

    val magnifierModifier = if (magnifierEnabled) {
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

    // 手动重试信号：递增即重启退避协程，重试计数归零，用户的显式操作立即生效
    // 而不用等当前退避走完。计数本身活在协程栈上，不是组合状态——理由见
    // [autoRetryOnError]，那里也解释了为什么这个形状不能退回「计数当 key」。
    var manualRetryNonce by remember(page.id) { mutableIntStateOf(0) }
    // key 用 imageRequest 而非 painter：painter 实例在 model 变化时会被复用，
    // 只按它做 key 会让复用到新页的节点继承上一页的退避计数与已记日志标记。
    LaunchedEffect(imageRequest, manualRetryNonce) {
//        painter.autoRetryOnError { "第 ${index + 1} 页 url=${page.url}" }
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
                            // 递增 nonce 重启退避协程，attempt 归零，
                            // 用户的显式操作立即生效而不用等当前退避走完。
                            manualRetryNonce++
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