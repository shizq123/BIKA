package com.shizq.bika.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.shizq.bika.core.data.model.DetailedReadingHistory
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComicCard(
    detailedReadingHistory: DetailedReadingHistory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    ElevatedCard(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
    ) {
        Row(
            modifier = Modifier
                .height(androidx.compose.foundation.layout.IntrinsicSize.Max)
                .defaultMinSize(minHeight = 135.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.TopStart) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(detailedReadingHistory.history.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${detailedReadingHistory.history.title} Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(100.dp)
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(8.dp))
                )

                if (detailedReadingHistory.history.isFavourited) {
                    Text(
                        text = "已收藏",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier
                            .padding(top = 4.dp, start = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                // 标题和作者
                Column(modifier = Modifier.weight(1f)) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // 状态徽章墙 (标题上方)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        val lastProgress = detailedReadingHistory.lastReadChapterProgress
                        if (lastProgress != null) {
                            val epsCount = detailedReadingHistory.history.epsCount
                            if (epsCount > lastProgress.chapterNumber) {
                                Badge(text = "有更新", containerColor = androidx.compose.ui.graphics.Color(0xFFFF9800))
                            } else if (lastProgress.chapterNumber >= epsCount && lastProgress.currentPage >= lastProgress.pageCount && lastProgress.pageCount > 0) {
                                Badge(text = "已读完", containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                            } else {
                                Badge(text = "已阅读", containerColor = MaterialTheme.colorScheme.secondary)
                            }
                        }

                        if (detailedReadingHistory.history.finished) {
                            Badge(text = "已完结", containerColor = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Text(
                        text = buildString {
                            if (detailedReadingHistory.history.pagesCount > 0) {
                                append("[${detailedReadingHistory.history.pagesCount}P] ")
                            }
                            append(detailedReadingHistory.history.title)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (detailedReadingHistory.history.finished) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = detailedReadingHistory.history.author,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 药丸式分类 Tag 标签
                    val categories = detailedReadingHistory.history.categories
                    if (categories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            categories.take(3).forEach { category ->
                                Text(
                                    text = category,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                val lastProgress = detailedReadingHistory.lastReadChapterProgress

                // 只有在有阅读进度时才显示进度部分
                if (lastProgress != null) {
                    Column {
                        val progressText =
                            "读至: 第${lastProgress.chapterNumber}话 ${lastProgress.currentPage}/${lastProgress.pageCount}页"
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 直接使用模型中计算好的百分比
                        val progress = detailedReadingHistory.lastReadChapterProgressPercentage

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            strokeCap = StrokeCap.Round,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    // 如果没有进度，可以显示“未开始”或留白
                    Text(
                        text = "未开始阅读",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 底部：相对时间与数据指标并列
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：最后阅读时间
                    Text(
                        text = formatRelativeTime(detailedReadingHistory.history.lastInteractionAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // 右侧：数据指标展示
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (detailedReadingHistory.history.totalLikes > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FavoriteBorder,
                                    contentDescription = "点赞",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = detailedReadingHistory.history.totalLikes.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        if (detailedReadingHistory.history.epsCount > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                    contentDescription = "章节",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "${detailedReadingHistory.history.epsCount}话",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun formatRelativeTime(instant: Instant): String {
    return remember(instant) {
        val now = Clock.System.now()
        val duration = now - instant

        when {
            duration < 1.minutes -> "刚刚"
            duration < 1.hours -> "${duration.inWholeMinutes}分钟前"
            duration < 24.hours -> "${duration.inWholeHours}小时前"
            duration < 2.days -> {
                val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                "昨天 ${
                    localDateTime.hour.toString().padStart(2, '0')
                }:${localDateTime.minute.toString().padStart(2, '0')}"
            }

            else -> {
                val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                "${localDateTime.month.number}月${localDateTime.day}日"
            }
        }
    }
}

@Composable
private fun Badge(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color
) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(containerColor)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}