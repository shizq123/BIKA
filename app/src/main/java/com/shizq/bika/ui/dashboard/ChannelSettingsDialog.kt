package com.shizq.bika.ui.dashboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.core.model.Channel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ChannelSettingsDialog(
    onDismiss: () -> Unit,
    viewModel: ChannelSettingsViewModel = hiltViewModel(),
) {
    val channelSettingsUiState by viewModel.userChannelPreferences.collectAsStateWithLifecycle()

    ChannelSettingsDialogContent(
        channels = channelSettingsUiState,
        onDismiss = onDismiss,
        onSave = viewModel::saveChannelSettings
    )
}

@Composable
fun ChannelSettingsDialogContent(
    channels: List<Channel>,
    onDismiss: () -> Unit,
    onSave: (List<Channel>) -> Unit
) {
    val data = remember(channels) { channels.toMutableStateList() }

    val listState = rememberLazyListState()
    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            data.apply {
                add(to.index, removeAt(from.index))
            }
        },
        lazyListState = listState
    )

    val windowInfo = LocalWindowInfo.current

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .widthIn(max = windowInfo.containerDpSize.width - 80.dp)
            .height(400.dp),
        onDismissRequest = onDismiss,
        title = {
            Text("频道显示设置", style = MaterialTheme.typography.titleLarge)
        },
        text = {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = data,
                        key = { it.displayName }
                    ) { channel ->
                        ReorderableItem(
                            state = state, key = channel.displayName,
                        ) { isDragging ->
                            val elevation by animateDpAsState(
                                targetValue = if (isDragging) 8.dp else 0.dp,
                                label = "elevation"
                            )

                            val scale by animateFloatAsState(
                                targetValue = if (isDragging) 1.05f else 1f,
                                label = "scale"
                            )
                            val bgColor =
                                if (isDragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent

                            ChannelSettingItem(
                                name = channel.displayName,
                                isActive = channel.isActive,
                                isDragging = isDragging,
                                modifier = Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        shadowElevation = elevation.toPx()
                                        shape = RoundedCornerShape(8.dp)
                                        clip = false
                                    }
                                    .background(bgColor, RoundedCornerShape(8.dp)),
                                onToggle = { isChecked ->
                                    val index = data.indexOf(channel)
                                    if (index != -1) {
                                        data[index] = channel.copy(isActive = isChecked)
                                    }
                                },
                                dragHandleModifier = Modifier.draggableHandle()
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                thickness = 0.5.dp,
                                color = if (isDragging) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(data.toList())
                    onDismiss()
                }
            ) {
                Text("完成")
            }
        }
    )
}

@Composable
private fun ChannelSettingItem(
    name: String,
    isActive: Boolean,
    isDragging: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isDragging) { onToggle(!isActive) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = isActive,
                onCheckedChange = null,
                modifier = Modifier.scale(0.8f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = dragHandleModifier
                    .padding(8.dp)
            )
        }
    }
}