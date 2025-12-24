package com.shizq.bika.ui.reader.bar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.shizq.bika.core.model.ReadingMode

@Composable
fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    readingMode: ReadingMode,
    onSeekToPage: (Int) -> Unit,
    onToggleChapterList: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReadingMode: () -> Unit,
    onOpenOrientation: () -> Unit
) {
    BottomBar(
        progressIndicator = {
            if (totalPages > 0) {
                Text(text = "${currentPage + 1} / $totalPages")
            }
        },
        progressSlider = {
            var sliderPosition by remember { mutableFloatStateOf(currentPage.toFloat()) }

            LaunchedEffect(currentPage) {
                sliderPosition = currentPage.toFloat()
            }
            Slider(
                value = sliderPosition,
                onValueChange = {
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    onSeekToPage(sliderPosition.toInt())
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
            }
        },
        endActions = {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, "设置")
            }
        }
    )
}