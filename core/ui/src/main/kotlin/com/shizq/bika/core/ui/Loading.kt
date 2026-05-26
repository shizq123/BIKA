package com.shizq.bika.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
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
    trackColor: Color = Color.Transparent, // 默认无底轨以凸显转动
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    val transition = rememberInfiniteTransition(label = "loading")
    
    // 旋转动画：1.2秒转一圈
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "rotation"
    )
    
    // 弧度伸缩动画：1.2秒一个周期
    val rawSweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "rawSweep"
    )
    
    // 计算当前弧度（在 30 度到 270 度之间来回拉伸和收缩）
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
        modifier = modifier.size(40.dp)
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


