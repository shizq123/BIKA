package com.shizq.bika.ui.comicinfo.page

import android.icu.text.DecimalFormat
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import coil3.compose.AsyncImage
import com.shizq.bika.R
import com.shizq.bika.ui.comicinfo.ComicDetail
import com.shizq.bika.ui.comicinfo.ComicSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicDetailPage(
    detail: ComicDetail,
    modifier: Modifier = Modifier,
    recommendations: List<ComicSummary>,
    onFavoriteClick: () -> Unit = {},
    onLikedClick: () -> Unit = {},
    navigationToReader: () -> Unit = {},
    navigationToSearch: (String, String, String) -> Unit = { _, _, _ -> },
    navigationToComicInfo: (String) -> Unit = {},
) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MediaSummary(
                coverUrl = detail.cover,
                title = detail.title,
                isFinished = detail.finished,
                view = detail.totalViews,
                chineseTeam = detail.chineseTeam,
                onTranslateClick = {
                    navigationToSearch("translate", it, it)
                }
            )

            ContentSummary(
                avatarUrl = detail.creator.avatar.originalImageUrl,
                creator = detail.creator.name,
                author = detail.author,
                description = detail.description,
                isLiked = detail.isLiked,
                likeCount = detail.totalLikes,
                onLikedClick = onLikedClick,
                onUploaderClick = {
                    navigationToSearch(
                        "knight",
                        detail.creator.name,
                        detail.creator.id,
                    )
                },
                onAuthorClick = { navigationToSearch("author", detail.author, detail.author) }
            )
            val all = detail.tags + detail.categories
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy((-8).dp),
            ) {
                all.fastForEach {
                    SuggestionChip(
                        { navigationToSearch("tags", it, it) },
                        label = {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    )
                }
            }

            HorizontalMultiBrowseCarousel(
                state = rememberCarouselState { recommendations.count() },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 16.dp, bottom = 16.dp),
                preferredItemWidth = 186.dp,
                itemSpacing = 8.dp,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) { i ->
                val item = recommendations[i]
                AsyncImage(
                    item.coverUrl,
                    modifier = Modifier
                        .height(205.dp)
                        .maskClip(MaterialTheme.shapes.extraLarge)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            navigationToComicInfo(item.id)
                        },
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        MangaBottomBar(
            isFavorited = detail.isFavourited,
            modifier = Modifier.align(Alignment.BottomCenter),
            onFavoriteClick = onFavoriteClick,
            onReadClick = navigationToReader,
        )
    }
}

@Composable
fun MediaSummary(
    coverUrl: String,
    title: String,
    isFinished: Boolean,
    view: Int,
    chineseTeam: String,
    modifier: Modifier = Modifier,
    onTranslateClick: (String) -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(3f / 4f)
                .clip(ShapeDefaults.Medium)
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectionContainer {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                ) {
                    Text(
                        if (isFinished) "已完结" else "连载中",
                        modifier = Modifier.padding(4.dp)
                    )
                }
                Card {
                    Text(
                        "人气 ${formatViews(view)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            if (chineseTeam.isNotEmpty()) {
                InfoLine(
                    label = "汉化组",
                    name = chineseTeam,
                    onClick = { onTranslateClick(chineseTeam) },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun LikeButton(
    isLiked: Boolean,
    likeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isLiked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isLiked) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) {
        Icon(
            modifier = Modifier,
            imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            contentDescription = "点赞"
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = likeCount.toString(),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun MediaSummaryPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            MediaSummary(
                coverUrl = "",
                title = "闪婚娇妻",
                isFinished = true,
                view = 329000,
                chineseTeam = "哔咔汉化组",
            )
        }
    }
}

private fun formatViews(count: Int): String {
    return if (count >= 10000) {
        val df = DecimalFormat("#.0")
        val num = count / 10000.0
        "${df.format(num)}万"
    } else {
        count.toString()
    }
}

@Composable
fun ContentSummary(
    avatarUrl: String,
    creator: String,
    author: String,
    description: String,
    isLiked: Boolean,
    likeCount: Int,
    modifier: Modifier = Modifier,
    onLikedClick: () -> Unit = {},
    onUploaderClick: () -> Unit = {},
    onAuthorClick: () -> Unit = {}
) {
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 头像保持不变
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    InfoLine(
                        label = stringResource(R.string.comic_author_label),
                        name = author,
                        onClick = onAuthorClick,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    InfoLine(
                        label = stringResource(R.string.comic_uploader_label),
                        name = creator,
                        onClick = onUploaderClick,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                LikeButton(
                    isLiked,
                    likeCount,
                    onClick = onLikedClick
                )
            }
        }

        SelectionContainer {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) { isDescriptionExpanded = !isDescriptionExpanded }
                    .animateContentSize()
            )
        }
    }
}

@Composable
private fun InfoLine(
    label: String,
    name: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick,
            )
    ) {
        SelectionContainer {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append("$label: ")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(name)
                    }
                },
                style = style,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun ContentSummaryPreview() {
    MaterialTheme {
        var isLiked by remember { mutableStateOf(false) }
        var likes by remember { mutableIntStateOf(1234) }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            ContentSummary(
                avatarUrl = "",
                author = "Takahashi",
                creator = "UploaderX",
                description = "这是一段非常非常长的漫画简介，默认情况下它应该只显示两行，但是当用户点击它的时候，它应该能够完全展开来显示所有的内容。这是一个很棒的用户体验优化。",
                isLiked = isLiked,
                likeCount = likes
            )
            HorizontalDivider()
            ContentSummary(
                avatarUrl = "",
                author = "Fujimoto",
                creator = "Fujimoto",
                description = "这是一个简短的描述。",
                isLiked = !isLiked,
                likeCount = likes
            )
        }
    }
}

@Composable
fun MangaBottomBar(
    isFavorited: Boolean,
    modifier: Modifier = Modifier,
    onFavoriteClick: () -> Unit = {},
    onReadClick: () -> Unit = {},
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val icon = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
            val text = if (isFavorited) "已收藏" else "收藏"
            val containerColor = if (isFavorited) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
            val contentColor = if (isFavorited) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
            FilledTonalButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            ) {
                Icon(imageVector = icon, contentDescription = text)
                Spacer(Modifier.width(4.dp))
                Text(text)
            }

            Button(
                onClick = onReadClick,
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("开始阅读", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}