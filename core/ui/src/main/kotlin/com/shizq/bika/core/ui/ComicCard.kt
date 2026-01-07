package com.shizq.bika.core.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import coil3.compose.AsyncImage
import com.shizq.bika.core.model.ComicSimple
import com.shizq.bika.core.model.Image2

@Composable
fun ComicCard(
    comic: ComicSimple,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit = {}
) {
    ListItem(
        modifier = modifier.clickable(onClick = onItemClick),
        headlineContent = {
            Text(
                text = "[${comic.pagesCount}]" + comic.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (comic.finished) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Unspecified
                }
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = comic.author,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    comic.categories.fastForEach { category ->
                        Tag(
                            text = category,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FavoriteBorder,
                        contentDescription = "点赞数",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = comic.totalLikes.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = "章节数",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Text(
                        text = comic.epsCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        },
        leadingContent = {
            AsyncImage(
                model = comic.image.originalImageUrl,
                contentDescription = comic.title,
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(3f / 4f)
                    .clip(MaterialTheme.shapes.small)
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun Tag(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .clip(CircleShape)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .padding(horizontal = 4.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun ComicCardPreview() {
    MaterialTheme {
        ComicCard(
            comic = ComicSimple(
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
                image = Image2(
                    fileServer = "https://example.com",
                    path = "thumb.jpg"
                )
            )
        )
    }
}
