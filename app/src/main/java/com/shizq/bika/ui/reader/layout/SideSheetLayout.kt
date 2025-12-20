package com.shizq.bika.ui.reader.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideSheetLayout(
    title: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    navigationButton: @Composable () -> Unit = { },
    closeButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    // Compose does not yet support side sheets
    // https://m3.material.io/components/side-sheets/overview

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(BottomSheetDefaults.windowInsets)
            .clickable(
                onClick = onDismissRequest,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ),
        contentAlignment = Alignment.TopStart,
    ) {
        // Layout guideline:
        // https://m3.material.io/components/side-sheets/guidelines#96245186-bae4-4a33-b41f-17833bb2e2d7

        Surface(
            modifier
                .clickable(
                    onClick = { }, // just to intercept clicks
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                )
                .fillMaxHeight()
                .widthIn(min = 300.dp, max = 400.dp)
                .width((this.maxWidth * 0.28f)),
            color = containerColor,
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier
                            .padding(start = 16.dp, end = 12.dp)
                            .padding(vertical = 16.dp)
                            .weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.outline) {
                            navigationButton()
                        }

                        Row(Modifier.weight(1f)) {
                            ProvideTextStyleContentColor(
                                MaterialTheme.typography.titleLarge,
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            ) {
                                title()
                            }
                        }
                    }

                    Box(Modifier.padding(start = 12.dp)) {
                        closeButton()
                    }
                }

                content()
            }
        }
    }
}

@Composable
fun ProvideTextStyleContentColor(
    value: TextStyle,
    color: Color = LocalContentColor.current,
    content: @Composable () -> Unit
) {
    val mergedStyle = LocalTextStyle.current.merge(value)
    CompositionLocalProvider(
        LocalTextStyle provides mergedStyle,
        LocalContentColor provides color, content = content,
    )
}
