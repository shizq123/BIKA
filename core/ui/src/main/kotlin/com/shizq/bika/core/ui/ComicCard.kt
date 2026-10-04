package com.shizq.bika.core.ui

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import com.shizq.bika.core.model.ComicSummary
import com.shizq.bika.core.model.RemoteImage

@Composable
fun ComicCard(
    comic: ComicSummary,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit = {},
) {
    ElevatedCard(
        onClick = onItemClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 0.dp,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ComicCover(
                comic = comic,
                placeholderColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(128.dp),
            ) {
                StatusBadges(comic)

                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = comic.author,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (comic.categories.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    CategoryRow(comic.categories)
                }

                Spacer(Modifier.weight(1f))

                ComicMetadata(comic)
            }
        }
    }
}

@Composable
private fun ComicCover(
    comic: ComicSummary,
    placeholderColor: Color,
) {
    Box(contentAlignment = Alignment.TopStart) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(comic.image.originalImageUrl)
                .placeholder(ColorDrawable(placeholderColor.toArgb()))
                .error(ColorDrawable(placeholderColor.toArgb()))
                .build(),
            contentDescription = comic.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(96.dp)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(10.dp)),
        )

        if (comic.isFavourited) {
            StatusBadge(
                text = "已收藏",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(5.dp),
            )
        }
    }
}

@Composable
private fun StatusBadges(comic: ComicSummary) {
    if (!comic.finished) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(bottom = 7.dp),
    ) {
        StatusBadge(
            text = "已完结",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun CategoryRow(categories: List<String>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categories.take(2).fastForEach { category ->
            CategoryTag(category)
        }
        if (categories.size > 2) {
            Text(
                text = "+${categories.size - 2}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ComicMetadata(comic: ComicSummary) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetadataItem(
            icon = Icons.Rounded.FavoriteBorder,
            contentDescription = "点赞数",
            text = comic.totalLikes.toString(),
        )
        MetadataItem(
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            contentDescription = "章节数",
            text = "${comic.epsCount}话",
        )
        if (comic.pagesCount > 0) {
            Text(
                text = "${comic.pagesCount}P",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetadataItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = contentColor,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
private fun CategoryTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .widthIn(max = 82.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}


@Preview(showBackground = true)
@Composable
private fun ComicCardPreview() {
    MaterialTheme {
        ComicCard(
            comic = ComicSummary(
                id = "1",
                title = "葬送的芙莉莲",
                author = "山田钟人 / 阿部司",
                totalViews = 1205000,
                totalLikes = 45200,
                pagesCount = 200,
                epsCount = 128,
                finished = false,
                categories = listOf("冒险", "奇幻", "剧情"),
                tags = listOf("魔法", "治愈", "精灵"),
                image = RemoteImage(
                    fileServer = "https://example.com",
                    path = "thumb.jpg"
                )
            )
        )
    }
}
