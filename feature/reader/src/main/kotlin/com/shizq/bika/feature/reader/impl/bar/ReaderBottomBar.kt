package com.shizq.bika.feature.reader.impl.bar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.shizq.bika.core.model.reader.ReadingMode
import com.shizq.bika.feature.reader.impl.util.ScrubState

@Composable
internal fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    readingMode: ReadingMode,
    onSeekToPage: (Int) -> Unit,
    onToggleChapterList: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReadingMode: () -> Unit,
    onOpenOrientation: () -> Unit,
    hasPrevChapter: Boolean = false,
    hasNextChapter: Boolean = false,
    onPrevChapter: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    scrubState: ScrubState,
) {
    DisposableEffect(Unit) {
        onDispose { scrubState.cancelScrub() }
    }

    BottomBar(
        progressIndicator = {
            if (totalPages > 0) {
                Text(text = "${currentPage + 1} / $totalPages")
            }
        },
        progressSlider = {
            Slider(
                value = scrubState.position,
                onValueChange = scrubState::onScrub,
                onValueChangeFinished = {
                    onSeekToPage(scrubState.onScrubFinished())
                },
                valueRange = 0f..(totalPages.coerceAtLeast(1) - 1).toFloat(),
            )
        },
        startActions = {
            IconButton(onClick = onToggleChapterList) {
                Icon(Icons.Rounded.Menu, "目录")
            }
        },
        middleActions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 上一章
                IconButton(
                    onClick = onPrevChapter,
                    enabled = hasPrevChapter
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "上一章",
                        tint = if (hasPrevChapter) LocalContentColor.current
                               else LocalContentColor.current.copy(alpha = 0.38f)
                    )
                }
                IconButton(onClick = onOpenReadingMode) {
                    val icon = when (readingMode) {
                        ReadingMode.LEFT_TO_RIGHT, ReadingMode.RIGHT_TO_LEFT -> Icons.Rounded.ViewCarousel
                        ReadingMode.WEBTOON -> Icons.Rounded.Smartphone
                        else -> Icons.Rounded.ViewColumn
                    }
                    Icon(icon, null)
                }
                IconButton(onClick = onOpenOrientation) {
                    Icon(Icons.Rounded.ScreenRotation, null)
                }
                // 下一章
                IconButton(
                    onClick = onNextChapter,
                    enabled = hasNextChapter
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "下一章",
                        tint = if (hasNextChapter) LocalContentColor.current
                               else LocalContentColor.current.copy(alpha = 0.38f)
                    )
                }
            }
        },
        endActions = {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, "设置")
            }
        }
    )
}