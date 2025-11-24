package com.shizq.bika.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shizq.bika.core.model.ReaderAction
import com.shizq.bika.core.model.TapZoneLayout
import kotlin.math.roundToInt

/**
 * 调试用遮罩层：可视化当前的点击区域 + 实时触摸反馈
 */
@Composable
fun DebugTouchAreaOverlay(
    mode: TapZoneLayout,
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    // 用于记录手指在屏幕上滑动的实时位置
    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    var currentAction by remember { mutableStateOf(ReaderAction.None) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { touchPoint = it },
                    onDragEnd = { touchPoint = null },
                    onDragCancel = { touchPoint = null },
                    onDrag = { change, _ ->
                        touchPoint = change.position
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchPoint = offset
                        tryAwaitRelease()
                        touchPoint = null
                    }
                )
            }
    ) {
        // 1. 绘制静态热力图网格
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val cols = 20
            val rows = 30
            val cellWidth = canvasWidth / cols
            val cellHeight = canvasHeight / rows

            for (i in 0 until cols) {
                for (j in 0 until rows) {
                    val centerX = i * cellWidth + cellWidth / 2
                    val centerY = j * cellHeight + cellHeight / 2

                    val action = mode.resolve(
                        x = centerX,
                        y = centerY,
                        width = canvasWidth.toInt(),
                        height = canvasHeight.toInt(),
                        isRtl = isRtl
                    )

                    drawRect(
                        color = getDebugColorForAction(action),
                        topLeft = Offset(i * cellWidth, j * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                }
            }

            // 绘制白色网格线
            val gridColor = Color.White.copy(alpha = 0.05f)
            for (i in 1 until cols) {
                drawLine(gridColor, Offset(i * cellWidth, 0f), Offset(i * cellWidth, canvasHeight))
            }
            for (j in 1 until rows) {
                drawLine(gridColor, Offset(0f, j * cellHeight), Offset(canvasWidth, j * cellHeight))
            }
        }

        // 2. 实时触摸反馈指示器
        touchPoint?.let { offset ->
            // 实时计算当前手指下的 Action
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { }
            ) {
            }

            Canvas(Modifier.fillMaxSize()) {
                currentAction = mode.resolve(
                    x = offset.x,
                    y = offset.y,
                    width = size.width.toInt(),
                    height = size.height.toInt(),
                    isRtl = isRtl
                )

                // 绘制触摸点十字准星
                drawLine(
                    Color.White,
                    Offset(offset.x, 0f),
                    Offset(offset.x, size.height),
                    strokeWidth = 2f
                )
                drawLine(
                    Color.White,
                    Offset(0f, offset.y),
                    Offset(size.width, offset.y),
                    strokeWidth = 2f
                )
                drawCircle(Color.White, radius = 20f, center = offset, style = Stroke(width = 4f))
            }

            // 3. 显示当前状态的文本标签 (跟随手指)
            val tagText = "${currentAction.name}\nX:${offset.x.toInt()} Y:${offset.y.toInt()}"
            Text(
                text = tagText,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 4f)
                ),
                modifier = Modifier
                    .offset { IntOffset(offset.x.roundToInt() + 30, offset.y.roundToInt() - 80) }
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(4.dp)
            )
        }

        // 4. 固定图例 (移到左上角，避免遮挡常见的中间操作区)
        DebugLegend(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 16.dp) // 避开状态栏
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        )
    }
}

private fun getDebugColorForAction(action: ReaderAction): Color {
    return when (action) {
        ReaderAction.NextPage -> Color(0xFF4CAF50).copy(alpha = 0.3f)
        ReaderAction.PrevPage -> Color(0xFFF44336).copy(alpha = 0.3f)
        ReaderAction.ToggleMenu -> Color(0xFF2196F3).copy(alpha = 0.3f)
        ReaderAction.None -> Color.Gray.copy(alpha = 0.2f)
    }
}

@Composable
private fun DebugLegend(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Touch Debugger", color = Color.White, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.size(4.dp))
        LegendItem(color = getDebugColorForAction(ReaderAction.NextPage), label = "Next Page")
        LegendItem(color = getDebugColorForAction(ReaderAction.PrevPage), label = "Prev Page")
        LegendItem(color = getDebugColorForAction(ReaderAction.ToggleMenu), label = "Menu")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color.copy(alpha = 1f))
                .border(1.dp, Color.White)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = Color.White, style = TextStyle(fontSize = 10.sp))
    }
}