package com.shizq.bika.ui.reader.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap

/**
 * 创建一个棋盘格样式的 [ShaderBrush]。
 *
 * 通常用于作为透明图片（如 PNG）的背景，模拟 Photoshop 的透明层显示效果。
 * 该函数使用了 [remember] 进行缓存，且利用 GPU 的 [TileMode.Repeated] 进行平铺，
 * 因此在 LazyColumn/LazyGrid 等列表中滚动使用时性能极佳。
 *
 * @param squareSize 单个方格的大小 (Dp)。会自动根据屏幕密度转换为像素。默认 10.dp。
 * @param color1 棋盘格的第一种颜色（通常是浅色/白色）。
 * @param color2 棋盘格的第二种颜色（通常是浅灰色）。
 * @return 一个自动平铺的画笔，可直接传给 Modifier.background()。
 */
@Composable
fun rememberCheckerboardBrush(
    squareSize: Dp = 10.dp,
    color1: Color = Color.White,
    color2: Color = Color.LightGray
): ShaderBrush {
    val density = LocalDensity.current
    return remember(squareSize, color1, color2) {
        val sizePx = with(density) { squareSize.toPx() }.toInt()
        val bitmap = createBitmap(sizePx * 2, sizePx * 2).asImageBitmap()
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // 绘制 Color1 (左上角 & 右下角)
        paint.color = color1
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)
        canvas.drawRect(sizePx.toFloat(), sizePx.toFloat(), sizePx * 2f, sizePx * 2f, paint)

        // 绘制 Color2 (右上角 & 左下角)
        paint.color = color2
        canvas.drawRect(sizePx.toFloat(), 0f, sizePx * 2f, sizePx.toFloat(), paint)
        canvas.drawRect(0f, sizePx.toFloat(), sizePx.toFloat(), sizePx * 2f, paint)

        // TileMode.Repeated 在水平和垂直方向上无限重复这个 2x2 的 Bitmap
        ShaderBrush(
            ImageShader(
                bitmap,
                TileMode.Repeated,
                TileMode.Repeated
            )
        )
    }
}