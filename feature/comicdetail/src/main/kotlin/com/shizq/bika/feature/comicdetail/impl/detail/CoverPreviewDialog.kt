package com.shizq.bika.feature.comicdetail.impl.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shizq.bika.core.ui.RetryableAsyncImage
import me.saket.telephoto.zoomable.EnabledZoomGestures
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

@Suppress("DEPRECATION")
@Composable
internal fun CoverPreviewDialog(
    cover: String,
    title: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val zoomableState = rememberZoomableState(ZoomSpec(maxZoomFactor = 4f))
        var imageSize by remember(cover) { mutableStateOf<Size?>(null) }
        SideEffect {
            zoomableState.setContentLocation(
                ZoomableContentLocation.scaledToFitAndCenterAligned(imageSize ?: Size.Unspecified),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .safeDrawingPadding()
                .clipToBounds(),
        ) {
            RetryableAsyncImage(
                model = cover,
                contentDescription = title.ifBlank { "封面大图" },
                contentScale = ContentScale.Fit,
                onSuccess = { state ->
                    val image = state.result.image
                    imageSize = if (image.width > 0 && image.height > 0) {
                        Size(image.width.toFloat(), image.height.toFloat())
                    } else {
                        null
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zoomable(
                        state = zoomableState,
                        gestures = EnabledZoomGestures.ZoomAndPan,
                    ),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭封面大图",
                    tint = Color.White,
                )
            }
        }
    }
}
