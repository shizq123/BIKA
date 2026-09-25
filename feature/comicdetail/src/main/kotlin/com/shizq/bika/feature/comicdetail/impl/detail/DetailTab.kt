package com.shizq.bika.feature.comicdetail.impl.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.core.ui.RetryableAsyncImage
import com.shizq.bika.feature.comicdetail.impl.ComicDetail
import com.shizq.bika.feature.comicdetail.impl.ComicSummary
import com.shizq.bika.feature.comicdetail.impl.isComicFullyDownloaded

/**
 * 漫画详情页的详情分页。
 *
 * 视觉上采用「沉浸式 Hero + 卡片化内容」：封面、标题和核心数据优先展示，
 * 低频信息下沉到独立卡片，减少传统详情页的表单感。
 */
@Composable
fun DetailTab(
    detail: ComicDetail,
    recommendations: List<ComicSummary>,
    downloadTasks: List<DownloadTask>,
    chapterProgress: List<ChapterProgressEntity>,
    onFavoriteClick: () -> Unit,
    onLikedClick: () -> Unit,
    navigationToReader: (order: Int) -> Unit,
    onRecommendedComicClick: (String) -> Unit,
    onTranslateClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onUploaderClick: (String, String) -> Unit,
    onDownloadWholeComic: () -> Unit,
    navigationToTagBlock: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDownloaded = remember(downloadTasks, detail.epsCount) {
        isComicFullyDownloaded(downloadTasks, detail.epsCount)
    }
    val lastReadChapter = remember(chapterProgress) {
        chapterProgress.maxByOrNull { it.lastReadAt }
    }
    val readOrder = lastReadChapter?.chapterId ?: 1

    var isFavourited by remember(detail.id, detail.isFavourited) {
        mutableStateOf(detail.isFavourited)
    }
    var isLiked by remember(detail.id, detail.isLiked) {
        mutableStateOf(detail.isLiked)
    }

    ComicDetailBody(
        detail = detail,
        isFavourited = isFavourited,
        isLiked = isLiked,
        isDownloaded = isDownloaded,
        modifier = modifier,
        onRead = { navigationToReader(readOrder) },
        onToggleFavourite = {
            isFavourited = !isFavourited
            onFavoriteClick()
        },
        onToggleLike = {
            isLiked = !isLiked
            onLikedClick()
        },
        onAuthorClick = { onAuthorClick(detail.author) },
        onTeamClick = { onUploaderClick(detail.chineseTeam, detail.creator.id) },
        onTagClick = onTranslateClick,
        recommendations = recommendations,
        onRecommendedComicClick = onRecommendedComicClick,
        onTagLongClick = navigationToTagBlock,
        onDownloadWholeComic = onDownloadWholeComic,
    )
}

@Composable
private fun ComicDetailBody(
    detail: ComicDetail,
    isFavourited: Boolean,
    isLiked: Boolean,
    isDownloaded: Boolean,
    onRead: () -> Unit,
    onToggleFavourite: () -> Unit,
    onToggleLike: () -> Unit,
    onAuthorClick: () -> Unit,
    onTeamClick: () -> Unit,
    onTagClick: (String) -> Unit,
    recommendations: List<ComicSummary>,
    onRecommendedComicClick: (String) -> Unit,
    onTagLongClick: (String) -> Unit,
    onDownloadWholeComic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HeroHeader(
            detail = detail,
            isFavourited = isFavourited,
            onAuthorClick = onAuthorClick,
            onTeamClick = onTeamClick,
            onTagClick = onTagClick,
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ActionBar(
                isFavourited = isFavourited,
                isLiked = isLiked,
                isDownloaded = isDownloaded,
                onRead = onRead,
                onToggleFavourite = onToggleFavourite,
                onToggleLike = onToggleLike,
                onDownloadWholeComic = onDownloadWholeComic,
            )

            StatsCard(
                totalViews = detail.totalViews,
                totalLikes = detail.totalLikes + if (isLiked != detail.isLiked) {
                    if (isLiked) 1 else -1
                } else 0,
                commentsCount = detail.commentsCount,
            )

            DescriptionCard(description = detail.description)

            TagsCard(
                tags = (detail.tags + detail.categories).distinct(),
                onTagClick = onTagClick,
                onTagLongClick = onTagLongClick,
            )

            if (recommendations.isNotEmpty()) {
                RecommendationsCard(
                    recommendations = recommendations,
                    onComicClick = onRecommendedComicClick,
                )
            }

            MetadataCard(detail = detail)
            Spacer(Modifier.height(44.dp))
        }
    }
}

@Composable
private fun HeroHeader(
    detail: ComicDetail,
    isFavourited: Boolean,
    onAuthorClick: () -> Unit,
    onTeamClick: () -> Unit,
    onTagClick: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(318.dp),
    ) {
        // 用封面做背景，配合渐变遮罩制造沉浸式头图。
        RetryableAsyncImage(
            model = detail.cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.42f),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            RetryableAsyncImage(
                model = detail.cover,
                contentDescription = detail.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(122.dp)
                    .height(166.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusPill(
                    text = if (detail.finished) "已完结" else "连载中",
                    emphasized = true,
                )
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                ClickableLine(
                    label = "作者",
                    value = detail.author.ifBlank { "未知" },
                    onClick = onAuthorClick,
                )
                if (detail.chineseTeam.isNotBlank()) {
                    ClickableLine(
                        label = "汉化",
                        value = detail.chineseTeam,
                        onClick = onTeamClick,
                    )
                }
                detail.categories.firstOrNull()?.let { category ->
                    StatusPill(text = category, onClick = { onTagClick(category) })
                }
            }
        }
    }
}

@Composable
private fun ActionBar(
    isFavourited: Boolean,
    isLiked: Boolean,
    isDownloaded: Boolean,
    onRead: () -> Unit,
    onToggleFavourite: () -> Unit,
    onToggleLike: () -> Unit,
    onDownloadWholeComic: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onRead,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
        ) {
            Icon(Icons.Filled.MenuBook, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isDownloaded) "继续阅读" else "开始阅读", fontWeight = FontWeight.Bold)
        }

        RoundAction(
            selected = isFavourited,
            selectedIcon = Icons.Filled.Bookmark,
            unselectedIcon = Icons.Outlined.BookmarkBorder,
            selectedTint = MaterialTheme.colorScheme.primary,
            onClick = onToggleFavourite,
        )
        RoundAction(
            selected = isLiked,
            selectedIcon = Icons.Filled.ThumbUp,
            unselectedIcon = Icons.Outlined.ThumbUp,
            selectedTint = MaterialTheme.colorScheme.tertiary,
            onClick = onToggleLike,
        )
        DownloadAction(
            isDownloaded = isDownloaded,
            onClick = onDownloadWholeComic,
        )
    }
}

@Composable
private fun DownloadAction(
    isDownloaded: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (isDownloaded) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        border = BorderStroke(
            1.dp,
            if (isDownloaded) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = if (isDownloaded) "重新下载" else "下载整本",
                tint = if (isDownloaded) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun RoundAction(
    selected: Boolean,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedTint: Color,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(if (selected) 1.08f else 1f, label = "action_scale")
    Surface(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) selectedTint.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(
            1.dp,
            if (selected) selectedTint.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = null,
                tint = if (selected) selectedTint else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size((22 * scale).dp),
            )
        }
    }
}

@Composable
private fun StatsCard(totalViews: Int, totalLikes: Int, commentsCount: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(Icons.Filled.Visibility, formatCount(totalViews), "浏览")
            StatItem(Icons.Filled.ThumbUp, formatCount(totalLikes.coerceAtLeast(0)), "点赞")
            StatItem(Icons.Filled.Favorite, formatCount(commentsCount), "评论")
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(7.dp))
        Column {
            Text(value, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DescriptionCard(description: String) {
    var expanded by rememberSaveable(description) { mutableStateOf(false) }
    var hasOverflow by remember(description) { mutableStateOf(false) }

    DetailCard(title = "简介") {
        Text(
            text = description.ifBlank { "暂无简介" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) hasOverflow = it.hasVisualOverflow },
            modifier = Modifier.animateContentSize(),
        )
        if (hasOverflow || expanded) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(if (expanded) "收起简介" else "展开简介")
            }
        }
    }
}

@Composable
private fun TagsCard(
    tags: List<String>,
    onTagClick: (String) -> Unit,
    onTagLongClick: (String) -> Unit,
) {
    DetailCard(title = "标签") {
        if (tags.isEmpty()) {
            Text("暂无标签", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { tag ->
                    StatusPill(
                        text = tag,
                        onClick = { onTagClick(tag) },
                        onLongClick = { onTagLongClick(tag) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationsCard(
    recommendations: List<ComicSummary>,
    onComicClick: (String) -> Unit,
) {
    DetailCard(title = "猜你喜欢") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            recommendations.forEach { comic ->
                Column(
                    modifier = Modifier
                        .width(112.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onComicClick(comic.id) },
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    RetryableAsyncImage(
                        model = comic.coverUrl,
                        contentDescription = comic.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                    Text(
                        text = comic.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (comic.author.isNotBlank()) {
                        Text(
                            text = comic.author,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataCard(detail: ComicDetail) {
    DetailCard(title = "作品信息") {
        InfoGrid(
            "章节数" to detail.epsCount.toString(),
            "页数" to detail.pagesCount.toString(),
            "更新时间" to detail.updatedAt.ifBlank { "未知" },
            "创建时间" to detail.createdAt.ifBlank { "未知" },
        )
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                content()
            },
        )
    }
}

@Composable
private fun InfoGrid(vararg entries: Pair<String, String>) {
    entries.toList().chunked(2).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            row.forEach { (label, value) ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(value, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ClickableLine(label: String, value: String, onClick: () -> Unit) {
    Text(
        text = "$label · $value",
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun StatusPill(
    text: String,
    emphasized: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val interactionModifier = when {
        onLongClick != null -> Modifier.combinedClickable(
            onClick = onClick ?: {},
            onLongClick = onLongClick,
        )

        onClick != null -> Modifier.clickable(onClick = onClick)
        else -> Modifier
    }

    Surface(
        modifier = interactionModifier,
        shape = RoundedCornerShape(50),
        color = if (emphasized) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (emphasized) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

private fun formatCount(value: Int): String = when {
    value >= 100_000_000 -> "%.1f亿".format(value / 100_000_000f)
    value >= 10_000 -> "%.1f万".format(value / 10_000f)
    else -> value.toString()
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F7F7)
@Composable
private fun ComicDetailSuccessPreview() {
    MaterialTheme {
        ComicDetailBody(
            detail = ComicDetail(
                id = "preview",
                title = "终将成为你：一段很长很长的漫画标题预览",
                author = "仲谷鳰",
                chineseTeam = "示例汉化组",
                description = "无法对他人产生特别情感的小糸侑，在初中毕业时被关系很好的男生告白，却无法给出回应。升入高中后，她遇见了学生会成员七海灯子，并逐渐理解了喜欢一个人的意义。",
                categories = listOf("百合", "校园"),
                tags = listOf("恋爱", "青春", "日常", "剧情"),
                epsCount = 45,
                pagesCount = 2180,
                finished = true,
                isFavourited = true,
                totalViews = 1_286_420,
                totalLikes = 32_680,
                commentsCount = 1_204,
                updatedAt = "2026-09-20",
                createdAt = "2024-03-15",
            ),
            isFavourited = true,
            isLiked = false,
            isDownloaded = false,
            onRead = {},
            onToggleFavourite = {},
            onToggleLike = {},
            onAuthorClick = {},
            onTeamClick = {},
            onTagClick = {},
            recommendations = emptyList(),
            onRecommendedComicClick = {},
            onTagLongClick = {},
            onDownloadWholeComic = {},
        )
    }
}
