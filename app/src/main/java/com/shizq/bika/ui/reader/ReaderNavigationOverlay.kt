package com.shizq.bika.ui.reader

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

/**
 * 阅读器操作区域覆盖层.
 *
 * 用于在初次进入或用户请求时，在屏幕上绘制操作热区（如：点击左边上一页，右边下一页）。
 *
 * @param navigation 包含区域信息的导航配置数据。
 * @param visible 是否显示覆盖层。
 * @param onDismissRequest 当用户点击覆盖层试图关闭它时的回调。
 */
@Composable
fun ReaderNavigationOverlay(
    navigation: ViewerNavigation?,
    visible: Boolean,
    onDismissRequest: () -> Unit
) {
    // 如果没有导航数据或者是禁用状态，直接不渲染
    if (navigation == null) return

    val context = LocalContext.current

    // 动画时长常量
    val fadeDuration = 1000

    // 准备 Paint 对象 (为了复用，虽然 Compose 中创建对象开销较小，但 Native Paint 还是建议 remember)
    // 1. 文字填充 Paint
    val textPaint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.WHITE
            textSize = 64f // 注意：这里单位是 px，如果需要适配屏幕密度，建议转换 sp to px
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    // 2. 文字描边 Paint
    val textBorderPaint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.BLACK
            textSize = 64f
            style = Paint.Style.STROKE
            strokeWidth = 8f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(fadeDuration)),
        exit = fadeOut(animationSpec = tween(fadeDuration))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 拦截点击事件，一旦点击即通知外部关闭
                .pointerInput(Unit) {
                    detectTapGestures {
                        onDismissRequest()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                navigation.regions.forEach { region ->
                    val rectF = region.rect

                    // 1. 计算当前屏幕上的像素坐标
                    // rectF 是 0.0 ~ 1.0 的比例坐标
                    val left = rectF.left * canvasWidth
                    val top = rectF.top * canvasHeight
                    val width = abs(rectF.right - rectF.left) * canvasWidth
                    val height = abs(rectF.bottom - rectF.top) * canvasHeight

                    // 2. 绘制半透明色块
                    drawRect(
                        color = region.type.color,
                        topLeft = Offset(left, top),
                        size = Size(width, height)
                    )

                    // 3. 绘制文字 (使用 Native Canvas 以支持描边效果)
                    drawIntoCanvas { canvas ->
                        val text = region.type.name

                        // 计算文字中心点
                        val centerX = left + (width / 2)
                        val centerY =
                            top + (height / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)

                        // 绘制描边
                        canvas.nativeCanvas.drawText(text, centerX, centerY, textBorderPaint)
                        // 绘制实体文字
                        canvas.nativeCanvas.drawText(text, centerX, centerY, textPaint)
                    }
                }
            }
        }
    }
}

data class ViewerNavigation(val regions: List<Region>)
data class Region(val rect: Rect, val type: RegionType)
data class RegionType(val name: String, val color: Color)