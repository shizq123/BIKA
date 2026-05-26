package com.shizq.bika.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.designsystem.theme.BikaTheme

@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun Loading(
    modifier: Modifier = Modifier,
    message: String = stringResource(id = R.string.core_ui_message_loading),
    contentAlignment: Alignment = Alignment.Center,
    style: TextStyle = MaterialTheme.typography.displaySmall,
) {
    Box(
        modifier = modifier,
        contentAlignment = contentAlignment,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(text = message, style = style)
        }
    }
}

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
    trackColor: Color = Color.Transparent,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    // 使用 withFrameNanos 驱动动画，完全绕过系统"动画程序时长缩放比例"设置
    // 即使系统动画缩放为 0x，动画依然正常运行
    var rotation by remember { mutableFloatStateOf(0f) }
    var rawSweep by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastFrameNanos = -1L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos > 0) {
                    val elapsedMs = (frameNanos - lastFrameNanos) / 1_000_000f
                    // 旋转：1333ms 一圈（Material Design 标准速度）
                    rotation = (rotation + elapsedMs * 360f / 1333f) % 360f
                    // 伸缩：600ms 一周期（与旋转错相，产生明显运动感）
                    rawSweep = (rawSweep + elapsedMs / 600f) % 1f
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    // 计算当前弧度（在 30° 到 270° 之间来回拉伸和收缩）
    val sweepAngle = if (rawSweep < 0.5f) {
        30f + (rawSweep / 0.5f) * 240f
    } else {
        270f - ((rawSweep - 0.5f) / 0.5f) * 240f
    }

    // 前半段起点固定，后半段起点向前移动以产生回弹效果
    val startAngleOffset = if (rawSweep < 0.5f) {
        0f
    } else {
        ((rawSweep - 0.5f) / 0.5f) * 240f
    }

    Canvas(
        modifier = Modifier.size(40.dp).then(modifier)
    ) {
        val strokeWidthPx = strokeWidth.toPx()
        val arcSize = size.minDimension - strokeWidthPx

        // 绘制背景轨道（如果有指定）
        if (trackColor != Color.Transparent) {
            drawCircle(
                color = trackColor,
                radius = arcSize / 2,
                center = center,
                style = Stroke(width = strokeWidthPx)
            )
        }

        // 绘制旋转且自动拉伸的圆弧
        drawArc(
            color = color,
            startAngle = rotation + startAngleOffset,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2),
            size = Size(arcSize, arcSize),
            style = Stroke(width = strokeWidthPx, cap = strokeCap)
        )
    }
}

@Preview
@Composable
private fun LoadingPreview() {
    BikaTheme {
        Loading()
    }
}
