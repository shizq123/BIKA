package com.shizq.bika.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * 漫画/图片阅读器的通用基础骨架 (Scaffold)。
 *
 * 该组件采用了**沉浸式**设计，并负责管理阅读器中各个 UI 图层的堆叠顺序 (Z-Order) 和交互事件的分发。
 *
 * ### 图层层级 (由下至上):
 * 1. **内容层 (Content)**: 放置 [content] (如 `HorizontalPager`, `LazyColumn`)。
 *    - 该层直接绑定了点击监听 ([onTap])。
 *    - 能够处理滑动 (Scroll/Drag) 事件，且不会被点击监听拦截。
 * 2. **悬浮信息层 (Floating Message)**: 放置 [floatingMessage] (如右下角的页码、时间、电量)。
 *    - 仅在菜单隐藏 ([showMenu] = false) 时显示。
 * 3. **加载层 (Loading)**: 放置 [loadingContent]，居中显示加载状态。
 * 4. **菜单层 (Overlay)**: 放置 [topBar] 和 [bottomBar]。
 *    - 带有进入/退出动画。
 *    - 内部已做点击拦截处理，防止点击菜单栏时误触下方的翻页逻辑。
 * 5. **侧边栏层 (Side Sheet)**: 放置 [sideSheet] (如章节目录)。
 *    - 位于最顶层，通常配合 ModalDrawer 或自定义动画使用。
 *
 * ### 交互逻辑说明:
 * - **滑动**: 滑动事件直接由 [content] 内部的组件消费（因为 `pointerInput` 不拦截 Scroll）。
 * - **点击**: 当用户点击屏幕且未被内部组件（如按钮）消费时，触发 [onTap] 回调。
 *
 * @param showMenu 控制顶部栏 ([topBar]) 和底部栏 ([bottomBar]) 的可见性。
 * @param modifier 应用于根布局的修饰符。
 * @param contentWindowInsets 窗口边距设置，默认为 [WindowInsets.safeContent]。
 * @param topBar 顶部菜单栏内容。通常包含返回按钮、标题等。
 * @param bottomBar 底部菜单栏内容。通常包含进度条、设置面板等。
 * @param content 阅读器的核心内容区域。**注意**：请勿在此处再次包裹 `Box` 处理点击，否则可能导致滑动冲突。
 * @param onTap 点击内容区域时的回调。
 *              - [Offset]: 点击位置的坐标 (相对于 content)。
 *              - [IntSize]: content 区域的总尺寸。
 *              - **用途**: 你可以在此回调中计算点击位置（例如：点击中间呼出菜单，点击左侧上一页，点击右侧下一页）。
 * @param floatingMessage 菜单隐藏时显示的悬浮挂件（例如："第 5/20 页"）。
 * @param loadingContent 加载中的提示内容。
 * @param sideSheet 侧滑菜单或设置面板内容。
 */
@Composable
fun ReaderScaffold(
    showMenu: Boolean,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.safeContent,
    topBar: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable ColumnScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit = {},
    onTap: (Offset, IntSize) -> Unit = { _, _ -> },
    floatingMessage: @Composable BoxScope.() -> Unit = {},
    loadingContent: @Composable BoxScope.() -> Unit = {},
    sideSheet: @Composable BoxScope.() -> Unit = {},
) {
    val enterTransition = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it / 2 }
    val exitTransition = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 }
    val bottomEnterTransition = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 }
    val bottomExitTransition = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // 1. 内容交互层 (Core Layer)
        BoxWithConstraints(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Transparent)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onTap(offset, size)
                    }
                }
        ) {
            content()
        }

        // 2. 悬浮指示器 (Floating Message)
        AnimatedVisibility(
            visible = !showMenu,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .windowInsetsPadding(contentWindowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.End))
            ) {
                ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                    floatingMessage()
                }
            }
        }

        // 3. 加载层
        Box(
            modifier = Modifier
                .matchParentSize()
                .windowInsetsPadding(contentWindowInsets),
            contentAlignment = Alignment.Center
        ) {
            loadingContent()
        }

        // 4. 菜单层 (UI Overlay) - 这一层不应该拦截非自身的点击
        // 使用 Box(matchParentSize) 可能会导致点击穿透问题，
        // 这里只让 TopBar 和 BottomBar 占据它们实际需要的空间，中间留空。
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            AnimatedVisibility(
                visible = showMenu,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                // 拦截点击，防止点击 TopBar 时触发下面的翻页/菜单隐藏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) { detectTapGestures {} }
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CompositionLocalProvider(LocalContentColor provides Color.White) {
                            topBar()
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Bottom Bar
            AnimatedVisibility(
                visible = showMenu,
                enter = bottomEnterTransition,
                exit = bottomExitTransition,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 同样拦截点击
                        .pointerInput(Unit) { detectTapGestures {} }
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        CompositionLocalProvider(LocalContentColor provides Color.White) {
                            bottomBar()
                        }
                    }
                }
            }
        }

        // 5. 侧边栏 (Side Sheet)
        Box(
            modifier = Modifier
                .matchParentSize()
                .windowInsetsPadding(contentWindowInsets.only(WindowInsetsSides.Vertical))
        ) {
            sideSheet()
        }
    }
}