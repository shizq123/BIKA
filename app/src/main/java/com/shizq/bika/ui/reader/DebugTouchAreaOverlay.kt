package com.shizq.bika.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shizq.bika.core.model.ReaderAction
import com.shizq.bika.core.model.TapZoneLayout

/**
 * 调试用遮罩层：可视化当前的点击区域
 * @param mode 当前的点击区域模式 (LShape, Kindle, Sides...)
 * @param isRtl 是否是从右向左阅读 (日漫模式)
 */
@Composable
fun DebugTouchAreaOverlay(
    mode: TapZoneLayout,
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 1. 定义采样精度 (列数和行数)
            val cols = 20
            val rows = 40

            val cellWidth = canvasWidth / cols
            val cellHeight = canvasHeight / rows

            // 2. 遍历每个网格
            for (i in 0 until cols) {
                for (j in 0 until rows) {
                    // 计算网格中心点坐标
                    val centerX = i * cellWidth + cellWidth / 2
                    val centerY = j * cellHeight + cellHeight / 2

                    val action = mode.resolve(
                        x = centerX,
                        y = centerY,
                        width = canvasWidth.toInt(),
                        height = canvasHeight.toInt(),
                        isRtl = isRtl
                    )

                    // 4. 根据动作获取颜色
                    val color = getDebugColorForAction(action)

                    // 5. 绘制色块
                    drawRect(
                        color = color,
                        topLeft = Offset(i * cellWidth, j * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                }
            }

            // 6. 绘制分割线 (可选，为了看清网格)
            drawRect(Color.White.copy(alpha = 0.1f), style = Stroke(width = 1f))
        }

        // 添加图例说明
        DebugLegend(
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(16.dp)
        )
    }
}

/**
 * 定义不同动作的调试颜色
 */
private fun getDebugColorForAction(action: ReaderAction): Color {
    return when (action) {
        ReaderAction.NextPage -> Color.Green.copy(alpha = 0.3f)   // 绿色 = 下一页
        ReaderAction.PrevPage -> Color.Red.copy(alpha = 0.3f)     // 红色 = 上一页
        ReaderAction.ToggleMenu -> Color.Blue.copy(alpha = 0.3f)  // 蓝色 = 菜单
        ReaderAction.None -> Color.Gray.copy(alpha = 0.3f)        // 灰色 = 无操作
    }
}

/**
 * 屏幕中间显示的图例，告诉用户什么颜色代表什么
 */
@Composable
private fun DebugLegend(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("区域调试模式", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.size(8.dp))
        LegendItem(color = Color.Green, label = "下一页 (Next)")
        LegendItem(color = Color.Red, label = "上一页 (Prev)")
        LegendItem(color = Color.Blue, label = "菜单 (Menu)")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color)
                .border(1.dp, Color.White)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = Color.White, style = TextStyle(fontSize = 12.sp))
    }
}