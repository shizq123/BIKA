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
 * 漫画/图片阅读器框架.
 *
 * @param onTap 当用户点击阅读区域时的回调。
 *              参数 [Offset] 为点击坐标，[IntSize] 为当前视图总大小。
 *              你可以根据坐标判断是点击了屏幕中央(呼出菜单)还是左右两侧(翻页)。
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