package com.shizq.bika.feature.reader.impl.autoscroll

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AutoScrollControlPanel(
    isScrolling: Boolean,
    speed: Int,
    onPlayPauseToggle: () -> Unit,
    onSpeedUp: () -> Unit,
    onSpeedDown: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.75f),
        contentColor = Color.White,
        modifier = modifier
            .padding(16.dp)
            .width(56.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            IconButton(onClick = onPlayPauseToggle) {
                Icon(
                    imageVector = if (isScrolling) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isScrolling) "暂停" else "开始",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onSpeedDown,
                enabled = speed > 1
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "减速",
                    tint = if (speed > 1) Color.White else Color.Gray
                )
            }

            Text(
                text = "v$speed",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(
                onClick = onSpeedUp,
                enabled = speed < 10
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "加速",
                    tint = if (speed < 10) Color.White else Color.Gray
                )
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "退出自动滚动",
                    tint = Color.Red
                )
            }
        }
    }
}