package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntSize
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.core.model.reader.ReaderAction
import com.shizq.bika.feature.reader.impl.gesture.GestureState
import com.shizq.bika.feature.reader.impl.gesture.VolumeKeyNavigation
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.EnabledZoomGestures
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

interface ReaderLayout {
    /**
     * 该布局是否自己处理缩放与点击。
     *
     * true 时宿主不再套容器级 `zoomable`——容器和页面同时注册缩放手势会互相
     * 抢事件，表现为捏合时页面乱跳。翻页模式让每页独立缩放，条漫模式仍由
     * 容器整体缩放（连续滚动下逐页缩放没有意义）。
     */
    val ownsPageGestures: Boolean get() = false

    @Composable
    fun Content(
        pageItems: LazyPagingItems<ChapterPage>,
        modifier: Modifier,
        onPageTap: (PageTapContext) -> Unit,
    )
}

@Composable
fun ReaderLayoutHost(
    readerContext: ReaderContext,
    gestureState: GestureState,
    pageItems: LazyPagingItems<ChapterPage>,
    toggleMenuVisibility: () -> Unit,
    onHideMenu: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val zoomableState = rememberZoomableState(ZoomSpec(maxZoomFactor = 4f))
    val currentReaderContext by rememberUpdatedState(readerContext)
    val currentGestureState by rememberUpdatedState(gestureState)
    val currentOnHideMenu by rememberUpdatedState(onHideMenu)
    val currentToggleMenu by rememberUpdatedState(toggleMenuVisibility)
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {

                    // 计算滑动的总距离 (包含 x 轴和 y 轴)
                    // getDistance() 等于 sqrt(x*x + y*y)
                    val distance = available.getDistance()

                    if (distance > 10f) {
                        currentOnHideMenu()
                    }
                }
                return Offset.Zero
            }
        }
    }
    // 回调是 suspend 的，VolumeKeyNavigation 内部已负责调度，这里不再套 scope.launch
    VolumeKeyNavigation(
        enabled = readerContext.config.volumeKeyNavigation,
        onVolumeUp = {
            currentOnHideMenu()
            currentReaderContext.controller.scrollPrevPage()
        },
        onVolumeDown = {
            currentOnHideMenu()
            currentReaderContext.controller.scrollNextPage()
        }
    )

    // 点击 -> 动作的映射只有这一处：容器级缩放和页面级缩放两条路径都汇到这里，
    // 避免翻页模式和条漫模式各写一套点击区判定后逐渐长歪。
    val onPageTap: (PageTapContext) -> Unit = remember {
        { tap ->
            when (currentGestureState.calculateAction(tap.position, tap.viewportSize)) {
                ReaderAction.NextPage -> scope.launch {
                    currentOnHideMenu()
                    currentReaderContext.controller.scrollNextPage()
                }

                ReaderAction.PrevPage -> scope.launch {
                    currentOnHideMenu()
                    currentReaderContext.controller.scrollPrevPage()
                }

                ReaderAction.ToggleMenu -> currentToggleMenu()
                ReaderAction.None -> Unit
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewSize = IntSize(constraints.maxWidth, constraints.maxHeight)
        val layout = readerContext.layout
        val gestureModifier = if (layout.ownsPageGestures) {
            Modifier
        } else {
            Modifier.zoomable(
                state = zoomableState,
                gestures = EnabledZoomGestures.ZoomAndPan,
                onClick = { offset ->
                    // 容器级路径：点击坐标本就是视口坐标，直接用。
                    onPageTap(PageTapContext(position = offset, viewportSize = viewSize))
                }
            )
        }
        key(layout::class) {
            layout.Content(
                pageItems = pageItems,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                    .then(gestureModifier),
                onPageTap = onPageTap,
            )
        }
    }
}
