package com.shizq.bika.ui.reader.bar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

//  bottomBar = {
//            // 使用单独的组件处理 Slider 逻辑，避免 ReaderContent 过度重组
//            ReaderBottomBar(
//                currentPageIndex = currentPageIndex,
//                pageCount = pageCount,
//                onPageJump = { targetPage ->
//                    onPageChange(targetPage)
//                    scope.launch {
//                        if (readingMode.viewerType == ViewerType.Pager) {
//                            pagerState.scrollToPage(targetPage)
//                        } else {
//                            listState.scrollToItem(targetPage)
//                        }
//                    }
//                },
//                onShowChapterList = { showChapterList = true },
//                onShowSettings = { showSettings = true }
//            )
@Composable
fun ReaderBottomBar(
    currentPageIndex: Int,
    pageCount: Int,
    onPageJump: (Int) -> Unit,
    onShowChapterList: () -> Unit,
    onShowSettings: () -> Unit
) {
    // 内部维护 Slider 的临时状态，防止拖动时 UI 抖动
    var sliderValue by remember(currentPageIndex) { mutableFloatStateOf(currentPageIndex.toFloat()) }

    BottomBar(
        progressIndicator = {
            if (pageCount > 0) {
                Text(text = "${sliderValue.toInt() + 1} / $pageCount")
            }
        },
        progressSlider = {
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    onPageJump(sliderValue.toInt())
                },
                valueRange = 0f..(pageCount.coerceAtLeast(1) - 1).toFloat()
            )
        },
        startActions = {
            IconButton(onClick = onShowChapterList) {
                Icon(Icons.Default.Menu, "目录")
            }
        },
        middleActions = {},
        endActions = {
            IconButton(onClick = onShowSettings) {
                Icon(Icons.Default.Settings, "设置")
            }
        }
    )
}

/**
 * 漫画阅读器的底部控制栏. 通常包含进度滑块、页码指示器以及章节切换按钮.
 *
 * 布局结构 (垂直排列):
 * 1. 进度行: [progressIndicator] (左) + [progressSlider] (右)
 * 2. 操作行: [startActions] (左) + [middleActions] (中) + [endActions] (右)
 *
 * @param startActions 最左侧的操作按钮 (例如: 目录, 设置)
 * @param middleActions 中间的操作按钮 (例如: 上一话, 下一话)
 * @param endActions 最右侧的操作按钮 (例如: 画面设置, 翻页模式)
 * @param progressIndicator 页码指示器文本 (例如: "12 / 40")
 * @param progressSlider 进度滑块. 会自动填充剩余宽度.
 */
@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    progressIndicator: @Composable () -> Unit,
    progressSlider: @Composable BoxScope.() -> Unit,
    startActions: @Composable RowScope.() -> Unit = {},
    middleActions: @Composable RowScope.() -> Unit = {},
    endActions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .clickable(remember { MutableInteractionSource() }, null, onClick = {})
                .padding(vertical = 4.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                    progressIndicator()
                }

                Spacer(Modifier.width(12.dp))

                Box(
                    Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    progressSlider()
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    startActions()
                }

                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    middleActions()
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    endActions()
                }
            }
        }
    }
}