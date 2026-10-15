package com.shizq.bika.feature.comicdetail.impl.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
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
import coil3.compose.AsyncImage
import com.shizq.bika.feature.comicdetail.impl.ComicDetail
import com.shizq.bika.feature.comicdetail.impl.ComicSummary
import kotlin.time.Instant

/** 漫画详情分页；Tab 导航由外层页面统一承载。 */
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
    listState: LazyListState = rememberLazyListState(),
) {
    ComicDetailBody(
        detail = detail,
        modifier = modifier,
        listState = listState,
        onAuthorClick = { onAuthorClick(detail.author) },
        onTeamClick = { onUploaderClick(detail.chineseTeam, detail.creator.id) },
        onTagClick = onTranslateClick,
        recommendations = recommendations,
        onRecommendedComicClick = onRecommendedComicClick,
        onTagLongClick = navigationToTagBlock,
    )
}

@OptIn(ExperimentalFoundationApi::class)
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
    listState: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        item(key = "hero") {
            ComicHero(detail, onAuthorClick, onTeamClick, onTagClick)
        }
        item(key = "stats") {
            ContentSection {
                StatsSection(detail.totalViews, detail.totalLikes, detail.commentsCount)
            }
        }
        item(key = "description") {
            SectionDivider()
            ContentSection { DescriptionSection(detail.description) }
        }
        item(key = "tags") {
            SectionDivider()
            ContentSection {
                TagsSection(
                    tags = (detail.tags + detail.categories).distinct(),
                    onTagClick = onTagClick,
                    onTagLongClick = onTagLongClick,
                )
            }
        }
        if (recommendations.isNotEmpty()) {
            item(key = "recommendations") {
                SectionDivider()
                ContentSection {
                    RecommendationsSection(recommendations, onRecommendedComicClick)
                }
            }
        }
        item(key = "metadata") {
            SectionDivider()
            ContentSection { MetadataSection(detail) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ContentSection(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        content()
    }
}

@Composable
private fun ComicHero(
    detail: ComicDetail,
    onAuthorClick: () -> Unit,
    onTeamClick: () -> Unit,
    onTagClick: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp),
    ) {
        AsyncImage(
            model = detail.cover,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .alpha(0.30f),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        )
        HeroHeader(detail, onAuthorClick, onTeamClick, onTagClick)
    }
}

@Composable
private fun HeroHeader(
    detail: ComicDetail,
    onAuthorClick: () -> Unit,
    onTeamClick: () -> Unit,
    onTagClick: (String) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 360.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeroCover(detail, Modifier
                    .width(138.dp)
                    .height(188.dp))
                HeroMetadata(
                    detail,
                    onAuthorClick,
                    onTeamClick,
                    onTagClick,
                    Modifier.fillMaxWidth(),
                    centered = true,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top,
            ) {
                HeroCover(detail, Modifier
                    .width(122.dp)
                    .height(166.dp))
                HeroMetadata(
                    detail,
                    onAuthorClick,
                    onTeamClick,
                    onTagClick,
                    Modifier.weight(1f),
                    centered = false,
                )
            }
        }
    }
}

@Composable
private fun HeroCover(detail: ComicDetail, modifier: Modifier) {
    AsyncImage(
        model = detail.cover,
        contentDescription = detail.title,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}

@Composable
private fun HeroMetadata(
    detail: ComicDetail,
    onAuthorClick: () -> Unit,
    onTeamClick: () -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier,
    centered: Boolean,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatusPill(if (detail.finished) "已完结" else "连载中", emphasized = true)
        Text(
            text = detail.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        ClickableLine("作者", detail.author.ifBlank { "未知" }, onAuthorClick, centered)
        if (detail.chineseTeam.isNotBlank()) {
            ClickableLine("汉化", detail.chineseTeam, onTeamClick, centered)
        }
        detail.categories.firstOrNull()?.let { category ->
            StatusPill(category, onClick = { onTagClick(category) })
        }
    }
}

@Composable
private fun StatsSection(totalViews: Int, totalLikes: Int, commentsCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatItem(Icons.Filled.Visibility, formatCount(totalViews), "浏览")
        StatItem(Icons.Filled.ThumbUp, formatCount(totalLikes.coerceAtLeast(0)), "点赞")
        StatItem(Icons.Filled.ChatBubble, formatCount(commentsCount), "评论")
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
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
private fun DescriptionSection(description: String) {
    var expanded by rememberSaveable(description) { mutableStateOf(false) }
    var hasOverflow by remember(description) { mutableStateOf(false) }

    SectionContent("简介") {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagsSection(
    tags: List<String>,
    onTagClick: (String) -> Unit,
    onTagLongClick: (String) -> Unit,
) {
    SectionContent("标签") {
        if (tags.isEmpty()) {
            Text("暂无标签", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                "点击查看相关内容 · 长按可屏蔽标签",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
private fun RecommendationsSection(
    recommendations: List<ComicSummary>,
    onComicClick: (String) -> Unit,
) {
    SectionContent("猜你喜欢") {
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
                    AsyncImage(
                        model = comic.coverUrl,
                        contentDescription = comic.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                    Text(
                        comic.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (comic.author.isNotBlank()) {
                        Text(
                            comic.author,
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
private fun MetadataSection(detail: ComicDetail) {
    SectionContent("作品信息") {
        InfoGrid(
            "章节数" to detail.epsCount.toString(),
            "页数" to detail.pagesCount.toString(),
            "更新时间" to formatDate(detail.updatedAt),
            "创建时间" to formatDate(detail.createdAt),
        )
    }
}

@Composable
private fun SectionContent(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
    )
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
private fun ClickableLine(
    label: String,
    value: String,
    onClick: () -> Unit,
    centered: Boolean = false,
) {
    Box(
        modifier = Modifier
            .then(if (centered) Modifier.wrapContentWidth() else Modifier.fillMaxWidth())
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$label · $value",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
        modifier = interactionModifier.heightIn(min = 40.dp),
        shape = RoundedCornerShape(50),
        color = if (emphasized) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (emphasized) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

private fun formatDate(instant: Instant): String = instant.toString().substringBefore('T')

private fun formatCount(value: Int): String = when {
    value >= 100_000_000 -> "%.1f亿".format(value / 100_000_000f)
    value >= 10_000 -> "%.1f万".format(value / 10_000f)
    else -> value.toString()
}

@Preview(showBackground = true)
@Composable
private fun ComicDetailSuccessPreview() {
    MaterialTheme {
        ComicDetailBody(
            detail = ComicDetail(
                id = "preview",
                title = "终将成为你：一段很长很长的漫画标题预览",
                author = "仲谷鳰",
                chineseTeam = "示例汉化组",
                description = "无法对他人产生特别情感的小糸侑，在初中毕业时被关系很好的男生告白，却无法给出回应。",
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
