package com.shizq.bika.feature.comicdetail.impl.detail

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.ui.RetryableAsyncImage
import com.shizq.bika.feature.comicdetail.impl.ComicDetail
import com.shizq.bika.feature.comicdetail.impl.ComicSummary
import kotlin.time.Instant

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
    onRecommendedComicClick: (String) -> Unit,
    onTranslateClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onUploaderClick: (String, String) -> Unit,
    navigationToTagBlock: (String) -> Unit,
    modifier: Modifier = Modifier,
    coverModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
) {
    ComicDetailBody(
        detail = detail,
        modifier = modifier,
        coverModifier = coverModifier,
        titleModifier = titleModifier,
        onAuthorClick = { onAuthorClick(detail.author) },
        onTeamClick = { onUploaderClick(detail.chineseTeam, detail.creator.id) },
        onTagClick = onTranslateClick,
        recommendations = recommendations,
        onRecommendedComicClick = onRecommendedComicClick,
        onTagLongClick = navigationToTagBlock,
    )
}

@Composable
private fun ComicDetailBody(
    detail: ComicDetail,
    onAuthorClick: () -> Unit,
    onTeamClick: () -> Unit,
    onTagClick: (String) -> Unit,
    recommendations: List<ComicSummary>,
    onRecommendedComicClick: (String) -> Unit,
    onTagLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    coverModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HeroHeader(
            detail = detail,
            onAuthorClick = onAuthorClick,
            onTeamClick = onTeamClick,
            onTagClick = onTagClick,
            coverModifier = coverModifier,
            titleModifier = titleModifier,
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatsCard(
                totalViews = detail.totalViews,
                totalLikes = detail.totalLikes,
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
        }
    }
}

@Composable
private fun HeroHeader(
    detail: ComicDetail,
    onAuthorClick: () -> Unit,
    onTeamClick: () -> Unit,
    onTagClick: (String) -> Unit,
    coverModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 188.dp),
    ) {
        // 背景只作为覆盖层，不参与 Hero 的测量，避免共享封面被固定头图高度向下推。
        RetryableAsyncImage(
            model = detail.cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .alpha(0.42f),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
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
                .align(Alignment.TopStart)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RetryableAsyncImage(
                model = detail.cover,
                contentDescription = detail.title,
                contentScale = ContentScale.Crop,
                modifier = coverModifier
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
                    modifier = titleModifier,
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
            "更新时间" to detail.updatedAt.toString(),
            "创建时间" to detail.createdAt.toString(),
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
                    fontWeight = FontWeight.Bold,
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
                updatedAt = Instant.parse("2026-09-20"),
                createdAt = Instant.parse("2024-03-15"),
            ),
            onAuthorClick = {},
            onTeamClick = {},
            onTagClick = {},
            recommendations = emptyList(),
            onRecommendedComicClick = {},
            onTagLongClick = {},
        )
    }
}