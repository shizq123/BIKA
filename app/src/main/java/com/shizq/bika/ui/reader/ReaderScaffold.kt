package com.shizq.bika.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 漫画/图片阅读器框架.
 *
 * 布局层级由下至上:
 *
 * - 内容层: [content] (Pager, LazyColumn, Webview 等)
 * - 交互层: [gestureHost] (用于检测点击屏幕中央呼出菜单，或处理边缘翻页区域)
 * - 信息层: [floatingIndicators] (页码、时间、电量等，当菜单隐藏时显示)
 * - 遮罩层: [topBar] 和 [bottomBar] (带有渐变背景)
 * - 侧边栏: [sideSheet] (例如章节列表)
 *
 * @param showMenu 当前菜单(TopBar/BottomBar)是否可见
 * @param content 阅读器的主要内容，例如 `HorizontalPager` 或 `LazyColumn`。
 * @param gestureHost 覆盖在内容之上的手势区域。通常用于检测点击事件以切换 [showMenu]。
 * @param topBar 顶部栏，包含返回、标题、设置等。
 * @param bottomBar 底部栏，包含进度条、下一话等。
 * @param floatingIndicators 悬浮指示器，例如右下角的页码/时间，通常在 [showMenu] 为 false 时显示。
 * @param sideSheet 侧边栏/抽屉，用于显示章节目录等。
 * @param contentWindowInsets 窗口边距设置。
 */
@Composable
fun ReaderScaffold(
    showMenu: Boolean,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.safeContent,
    topBar: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable ColumnScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit = {},
    gestureHost: @Composable BoxWithConstraintsScope.() -> Unit = {},
    floatingIndicators: @Composable BoxScope.() -> Unit = {},
    loadingContent: @Composable BoxScope.() -> Unit = {},
    sideSheet: @Composable BoxScope.() -> Unit = {},
) {
    // 定义进入和退出的动画，模拟 VideoScaffold 中的 MotionScheme
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
        // 1. 阅读器内容层 (最底层)
        // 这里不加 WindowInsets padding，让图片可以延伸到状态栏下方(沉浸式)
        Box(
            Modifier
                .background(Color.Transparent)
                .matchParentSize(), // no window insets for video
        ) {
            content()
            Box(Modifier.matchParentSize()) // 防止点击事件传播到 video 里
        }

        // 2. 手势交互层
        // 覆盖在内容之上，用于捕获点击屏幕中央呼出菜单的操作，或者处理特定的热区
        BoxWithConstraints(modifier = Modifier.matchParentSize()) {
            gestureHost()
        }

        // 3. 悬浮指示器 (当菜单隐藏时显示的页码、系统时间等)
        // 通常显示在右下角或底部
        AnimatedVisibility(
            visible = showMenu,
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
                    // 稍微加一点阴影或背景以防图片太白看不清，这里假设 floatingIndicators 自己处理了背景
                    floatingIndicators()
                }
            }
        }

        // 4. 加载/提示层
        Box(
            modifier = Modifier
                .matchParentSize()
                .windowInsetsPadding(contentWindowInsets),
            contentAlignment = Alignment.Center
        ) {
            loadingContent()
        }

        // 5. 控制器 UI 层 (TopBar & BottomBar)
        Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Top Bar Region
                AnimatedVisibility(
                    visible = showMenu,
                    enter = enterTransition,
                    exit = exitTransition,
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

                Spacer(Modifier.weight(1f))

                // Bottom Bar Region
                AnimatedVisibility(
                    visible = showMenu,
                    enter = bottomEnterTransition,
                    exit = bottomExitTransition,
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(contentWindowInsets.only(WindowInsetsSides.Bottom))
                    ) {
                        CompositionLocalProvider(LocalContentColor provides Color.White) {
                            bottomBar()
                        }
                    }
                }
            }
        }

        // 6. 侧边栏/抽屉层 (例如目录)
        // 通常覆盖在所有内容之上，除了系统手势区域
        // 这里使用 Box 包装，具体动画（如 slideIn）交给 sideSheet 内部实现或使用 ModalNavigationDrawer
        Box(
            modifier = Modifier
                .matchParentSize()
                .windowInsetsPadding(contentWindowInsets.only(WindowInsetsSides.Vertical))
        ) {
            sideSheet()
        }
    }
}