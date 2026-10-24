package com.shizq.bika.feature.comicdetail.impl.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.shizq.bika.core.designsystem.theme.BikaTheme
import com.shizq.bika.feature.comicdetail.impl.ComicDetail
import com.shizq.bika.feature.comicdetail.impl.ComicSummary
import com.shizq.bika.feature.comicdetail.impl.ComicReadingStatus
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
    readingStatus: ComicReadingStatus? = null,
) {
    ComicDetailBody(
        detail = detail,
        modifier = modifier,
        listState = listState,
        readingStatus = readingStatus,
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
    readingStatus: ComicReadingStatus? = null,
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        item(key = "hero") {
            ComicHero(detail, readingStatus, onAuthorClick, onTeamClick, onTagClick)
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
    readingStatus: ComicReadingStatus?,
    onAuthorClick: () -> Unit,
    onTeamClick: () -> Unit,
    onTagClick: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
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
        HeroHeader(detail, readingStatus, onAuthorClick, onTeamClick, onTagClick)
    }
}

@Composable
private fun HeroHeader(
    detail: ComicDetail,
    readingStatus: ComicReadingStatus?,
    onAuthorClick: () -> Unit,
    onTeamClick: () -> Unit,
    onTagClick: (String) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale >= 1.3f
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeroCover(detail, Modifier.width(128.dp).height(176.dp))
                    HeroMetadata(detail, readingStatus, onTagClick, Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeroCover(detail, Modifier.width(128.dp).height(176.dp))
                    HeroMetadata(detail, readingStatus, onTagClick, Modifier.weight(1f))
                }
            }
            CreatorCredits(detail, stacked, onAuthorClick, onTeamClick)
        }
    }
}

@Composable
private fun HeroCover(detail: ComicDetail, modifier: Modifier) {
    var showCoverPreview by rememberSaveable(detail.id, detail.cover) { mutableStateOf(false) }
    AsyncImage(
        model = detail.cover,
        contentDescription = detail.title,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(
                enabled = detail.cover.isNotBlank(),
                onClickLabel = "查看封面大图",
                role = Role.Button,
            ) { showCoverPreview = true },
    )
    if (showCoverPreview && detail.cover.isNotBlank()) {
        CoverPreviewDialog(
            cover = detail.cover,
            title = detail.title,
            onDismiss = { showCoverPreview = false },
        )
    }
}

@Composable
private fun HeroMetadata(
    detail: ComicDetail,
    readingStatus: ComicReadingStatus?,
    onTagClick: (String) -> Unit,
    modifier: Modifier,
) {
    val category = detail.categories.firstOrNull { it.isNotBlank() }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusPill(
            text = if (detail.finished) "已完结" else "连载中",
            emphasized = true,
        )
        Text(
            text = detail.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (readingStatus != null || category != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                readingStatus?.let { status ->
                    ReadingStatusBadge(status, Modifier.align(Alignment.CenterVertically))
                }
                category?.let {
                    StatusPill(
                        text = it,
                        onClick = { onTagClick(it) },
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingStatusBadge(status: ComicReadingStatus, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (label, container, foreground) = when (status) {
        ComicReadingStatus.HasUpdates -> Triple("有更新", colors.tertiaryContainer, colors.onTertiaryContainer)
        ComicReadingStatus.Completed -> Triple("已读完", colors.secondaryContainer, colors.onSecondaryContainer)
        ComicReadingStatus.Read -> Triple("已阅读", colors.surfaceContainerHighest, colors.onSurfaceVariant)
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = container, contentColor = foreground) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
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
    val uniqueRecommendations = remember(recommendations) { recommendations.distinctBy { it.id } }
    SectionContent("猜你喜欢") {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uniqueRecommendations, key = { it.id }, contentType = { "recommendation" }) { comic ->
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
private fun CreatorCredits(
    detail: ComicDetail,
    stacked: Boolean,
    onAuthorClick: () -> Unit,
    onTeamClick: () -> Unit,
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, dividerColor),
    ) {
        if (stacked || detail.chineseTeam.isBlank()) {
            Column {
                CreatorCredit(
                    label = "作者",
                    value = detail.author,
                    onClick = onAuthorClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (detail.chineseTeam.isNotBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = dividerColor)
                    CreatorCredit(
                        label = "汉化",
                        value = detail.chineseTeam,
                        onClick = onTeamClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                CreatorCredit(
                    label = "作者",
                    value = detail.author,
                    onClick = onAuthorClick,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                VerticalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = dividerColor,
                )
                CreatorCredit(
                    label = "汉化",
                    value = detail.chineseTeam,
                    onClick = onTeamClick,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun CreatorCredit(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clickable(enabled = value.isNotBlank(), role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = value.ifBlank { "未知" },
            color = if (value.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
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
    modifier: Modifier = Modifier,
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
        modifier = modifier.then(interactionModifier).heightIn(min = if (onClick != null || onLongClick != null) 36.dp else 28.dp),
        shape = RoundedCornerShape(8.dp),
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
            modifier = Modifier.padding(horizontal = 9.dp, vertical = if (onClick != null || onLongClick != null) 8.dp else 6.dp),
        )
    }
}

private fun formatDate(instant: Instant): String = instant.toString().substringBefore('T')

private fun formatCount(value: Int): String = when {
    value >= 100_000_000 -> "%.1f亿".format(value / 100_000_000f)
    value >= 10_000 -> "%.1f万".format(value / 10_000f)
    else -> value.toString()
}

@Preview(name = "详情 · 普通标题", showBackground = true, widthDp = 360)
@Composable
private fun ComicDetailSuccessPreview() {
    ComicDetailPreview()
}

@Preview(name = "详情 · 长文本", showBackground = true, widthDp = 360)
@Preview(name = "详情 · 窄屏大字号", showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun ComicDetailLongTextPreview() {
    ComicDetailPreview(
        title = "星海旅人：穿越遥远星系寻找失落文明的漫长旅程",
        author = "星野工作室（原作与联合绘画创作团队）",
        chineseTeam = "星光汉化组与远方联合翻译团队",
        readingStatus = ComicReadingStatus.HasUpdates,
    )
}

@Preview(name = "详情 · 无汉化信息", showBackground = true, widthDp = 360)
@Composable
private fun ComicDetailWithoutTeamPreview() {
    ComicDetailPreview(author = "", chineseTeam = "")
}

@Preview(name = "详情 · 深色", showBackground = true, widthDp = 360)
@Composable
private fun ComicDetailDarkPreview() {
    ComicDetailPreview(darkTheme = true, readingStatus = ComicReadingStatus.Completed)
}

@Composable
private fun ComicDetailPreview(
    title: String = "星海旅人",
    author: String = "星野工作室",
    chineseTeam: String = "星光汉化组",
    readingStatus: ComicReadingStatus? = null,
    darkTheme: Boolean = false,
) {
    BikaTheme(darkTheme = darkTheme, dynamicColor = false) {
        Surface {
            ComicDetailBody(
                detail = ComicDetail(
                    id = "preview",
                    title = title,
                    author = author,
                    chineseTeam = chineseTeam,
                    description = "一段关于星空、旅途与相遇的故事。",
                    categories = listOf("长篇", "冒险"),
                    tags = listOf("科幻", "旅行", "日常", "剧情"),
                    epsCount = 45,
                    pagesCount = 2180,
                    finished = true,
                    totalViews = 1_286_420,
                    totalLikes = 32_680,
                    commentsCount = 1_204,
                    updatedAt = Instant.parse("2026-09-20T00:00:00Z"),
                    createdAt = Instant.parse("2024-03-15T00:00:00Z"),
                ),
                onAuthorClick = {},
                onTeamClick = {},
                onTagClick = {},
                recommendations = emptyList(),
                onRecommendedComicClick = {},
                onTagLongClick = {},
                readingStatus = readingStatus,
            )
        }
    }
}
