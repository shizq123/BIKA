package com.shizq.bika.ui.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.model.Channel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ChannelSettingsDialog(
    channels: List<Channel>,
    onDismiss: () -> Unit,
    onChannelToggle: (Channel, Boolean) -> Unit,
    onOrderChange: (List<Channel>) -> Unit
) {
    val data = remember { channels.toMutableStateList() }

    val listState = rememberLazyListState()
    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            data.apply {
                add(to.index, removeAt(from.index))
            }
        },
        lazyListState = listState
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("频道显示设置", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
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
                            val elevation = animateDpAsState(
                                if (isDragging) 8.dp else 0.dp,
                                label = "elevation"
                            )
                            val bgColor =
                                if (isDragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent

                            ChannelSettingItem(
                                name = channel.displayName,
                                isActive = channel.isActive,
                                isDragging = isDragging,
                                modifier = Modifier
                                    .shadow(elevation.value)
                                    .background(bgColor, RoundedCornerShape(4.dp)),
                                onToggle = { isChecked ->
                                    onChannelToggle(channel, isChecked)
                                    val index = data.indexOf(channel)
                                    if (index != -1) {
                                        data[index] = channel.copy(isActive = isChecked)
                                    }
                                },
                                dragHandleModifier = Modifier.draggableHandle()
                            )

                            if (!isDragging) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOrderChange(data.toList())
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
            .clickable { if (!isDragging) onToggle(!isActive) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f) // 占满剩余空间
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
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