package com.shizq.bika.ui.comicinfo.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.designsystem.theme.BikaTheme

@Composable
fun MangaBottomBar(
    isFavorited: Boolean,
    modifier: Modifier = Modifier,
    showDownload: Boolean = false,
    isDownloaded: Boolean = false,
    isContinue: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onReadClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
) {
    Surface(
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val favorite = favoriteVisual(isFavorited)
            BottomBarButton(
                text = favorite.text,
                icon = favorite.icon,
                containerColor = favorite.containerColor,
                contentColor = favorite.contentColor,
                modifier = Modifier.weight(1f),
                onClick = onFavoriteClick,
            )

            if (showDownload) {
                val download = downloadVisual(isDownloaded)
                BottomBarButton(
                    text = download.text,
                    icon = download.icon,
                    containerColor = download.containerColor,
                    contentColor = download.contentColor,
                    modifier = Modifier.weight(1f),
                    enabled = !isDownloaded,
                    onClick = onDownloadClick,
                )
            }

            BottomBarButton(
                text = if (isContinue) "继续阅读" else "开始阅读",
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1.5f),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                onClick = onReadClick,
            )
        }
    }
}

@Composable
private fun BottomBarButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = text)
                Spacer(Modifier.width(4.dp))
            }
            Text(text = text, style = textStyle)
        }
    }
}

private data class ButtonVisual(
    val icon: ImageVector?,
    val text: String,
    val containerColor: Color,
    val contentColor: Color,
)

@Composable
private fun favoriteVisual(isFavorited: Boolean) = ButtonVisual(
    icon = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
    text = if (isFavorited) "已收藏" else "收藏",
    containerColor = if (isFavorited) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.secondaryContainer,
    contentColor = if (isFavorited) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSecondaryContainer,
)

@Composable
private fun downloadVisual(isDownloaded: Boolean) = ButtonVisual(
    icon = if (isDownloaded) Icons.Filled.Check else Icons.Filled.Download,
    text = if (isDownloaded) "已下载" else "下载",
    containerColor = if (isDownloaded) MaterialTheme.colorScheme.tertiaryContainer
    else MaterialTheme.colorScheme.secondaryContainer,
    contentColor = if (isDownloaded) MaterialTheme.colorScheme.onTertiaryContainer
    else MaterialTheme.colorScheme.onSecondaryContainer,
)

private data class BottomBarPreviewState(
    val isFavorited: Boolean = false,
    val showDownload: Boolean = false,
    val isDownloaded: Boolean = false,
    val isContinue: Boolean = false,
)

private class BottomBarPreviewStateProvider : PreviewParameterProvider<BottomBarPreviewState> {
    override val values: Sequence<BottomBarPreviewState> = sequenceOf(
        BottomBarPreviewState(),
        BottomBarPreviewState(showDownload = true),
        BottomBarPreviewState(isFavorited = true, showDownload = true, isDownloaded = true),
        BottomBarPreviewState(showDownload = true, isContinue = true),
    )
}

@Preview(name = "状态", group = "MangaBottomBar", showBackground = true)
@Preview(
    name = "暗色",
    group = "MangaBottomBar",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
)
@Preview(
    name = "大字体",
    group = "MangaBottomBar",
    showBackground = true,
    fontScale = 1.5f,
)
@Composable
private fun MangaBottomBarPreview(
    @PreviewParameter(BottomBarPreviewStateProvider::class) state: BottomBarPreviewState,
) {
    BikaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MangaBottomBar(
                isFavorited = state.isFavorited,
                showDownload = state.showDownload,
                isDownloaded = state.isDownloaded,
                isContinue = state.isContinue,
            )
        }
    }
}