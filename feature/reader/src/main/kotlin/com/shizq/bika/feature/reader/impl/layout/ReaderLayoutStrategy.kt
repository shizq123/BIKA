package com.shizq.bika.feature.reader.impl.layout

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.core.data.paging.ChapterPage
import com.shizq.bika.core.model.reader.TapAction
import com.shizq.bika.feature.reader.impl.gesture.GestureState
import com.shizq.bika.feature.reader.impl.gesture.VolumeKeyNavigation
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * 布局策略：负责页面怎么排、以及**自己**的缩放手势。
 *
 * 缩放归属不再由标志位协商。之前接口上有 `isGestureSelfContained`，宿主据此决定
 * 要不要套容器级 `zoomable`——两层都懂缩放、靠一个布尔约定谁退让，漏配的表现是
 * 捏合时画面乱跳。现在每个策略在自己内部套 zoomable：翻页模式每页独立缩放
 * （换页自动复位），条漫模式容器整体缩放。
 *
 * 宿主只保留一件事：把点击坐标解析成翻页/菜单动作（[onPageTap]），因为那需要
 * 阅读方向与点击分区配置，属于跨策略的共享规则。
 */
interface ReaderLayoutStrategy {
    @Composable
    fun RenderContent(
        pageItems: LazyPagingItems<ChapterPage>,
        modifier: Modifier,
        onPageTap: (PageTapContext) -> Unit,
    )
}

private enum class ReaderNavigationEvent {
    NextPage,
    PrevPage,
}

@Composable
fun ReaderLayoutHost(
    readerContext: ReaderContext,
    gestureState: GestureState,
    pageItems: LazyPagingItems<ChapterPage>,
    toggleMenuVisibility: () -> Unit,
    onHideMenu: () -> Unit,
) {
    val currentReaderContext by rememberUpdatedState(readerContext)
    val currentGestureState by rememberUpdatedState(gestureState)
    val currentOnHideMenu by rememberUpdatedState(onHideMenu)
    val currentToggleMenu by rememberUpdatedState(toggleMenuVisibility)
    val navigationEvents = remember(readerContext.controller) {
        Channel<ReaderNavigationEvent>(
            capacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    }

    LaunchedEffect(readerContext.controller, navigationEvents) {
        navigationEvents.receiveAsFlow().collect { event ->
            currentOnHideMenu()
            when (event) {
                ReaderNavigationEvent.NextPage -> currentReaderContext.controller.scrollNextPage()
                ReaderNavigationEvent.PrevPage -> currentReaderContext.controller.scrollPrevPage()
            }
        }
    }

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
    // 点击和音量键共用同一条导航事件流；执行期间只保留最新的一次导航意图。
    VolumeKeyNavigation(
        enabled = readerContext.config.volumeKeyNavigation,
        onVolumeUp = {
            navigationEvents.trySend(ReaderNavigationEvent.PrevPage)
        },
        onVolumeDown = {
            navigationEvents.trySend(ReaderNavigationEvent.NextPage)
        }
    )

    // 点击 -> 动作的映射只有这一处：两种布局的点击都汇到这里，
    // 避免翻页模式和条漫模式各写一套点击区判定后逐渐长歪。
    val onPageTap: (PageTapContext) -> Unit = remember(navigationEvents) {
        { tap ->
            when (currentGestureState.calculateAction(tap.position, tap.viewportSize)) {
                TapAction.NextPage -> navigationEvents.trySend(ReaderNavigationEvent.NextPage)
                TapAction.PrevPage -> navigationEvents.trySend(ReaderNavigationEvent.PrevPage)

                TapAction.ToggleMenu -> currentToggleMenu()
                TapAction.None -> Unit
            }
        }
    }

    // 不再需要 BoxWithConstraints 取视口尺寸：视口尺寸由产生点击的那一层
    // （页面自身或条漫容器）随 PageTapContext 一起给出。
    readerContext.layout.RenderContent(
        pageItems = pageItems,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        onPageTap = onPageTap,
    )
}
