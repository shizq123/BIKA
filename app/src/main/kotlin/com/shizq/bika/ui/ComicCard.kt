package com.shizq.bika.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.request.ImageRequest
import com.shizq.bika.core.data.model.DetailedReadingHistory
import com.shizq.bika.core.data.model.isCompleted
import com.shizq.bika.core.ui.RetryableAsyncImage
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
    onReadLatestClick: ((latestOrder: Int) -> Unit)? = null,
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
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(contentAlignment = Alignment.TopStart) {
                // 封面图：失败自动退避重试 + 点击重试（网络不稳定时不静默失败）
                val context = LocalContext.current
                RetryableAsyncImage(
                    model = remember(context, detailedReadingHistory.history.coverUrl) {
                        ImageRequest.Builder(context)
                            .data(detailedReadingHistory.history.coverUrl)
                            .build()
                    },
                    contentDescription = "${detailedReadingHistory.history.title} Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(100.dp)
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val lastProgress = detailedReadingHistory.lastReadChapterProgress

                ComicHistoryStatusBadges(detailedReadingHistory)

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

                Text(
                    text = detailedReadingHistory.history.author,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 分类标签在窄屏或大字体下按项换行。
                val categories = detailedReadingHistory.history.categories
                if (categories.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        categories.take(3).forEach { category ->
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .widthIn(max = 88.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (categories.size > 3) {
                            Text(
                                text = "+${categories.size - 3}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }

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

                        Spacer(modifier = Modifier.height(4.dp))

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
                    }
                } else {
                    // 如果没有进度，可以显示“未开始”或留白
                    Text(
                        text = "未开始阅读",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // 时间与统计按完整项目换行，避免章节数被逐字挤成竖排。
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = formatRelativeTime(detailedReadingHistory.history.lastInteractionAt),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    if (detailedReadingHistory.history.totalLikes > 0) {
                        HistoryMetadata(
                            icon = Icons.Rounded.FavoriteBorder,
                            description = "点赞",
                            text = detailedReadingHistory.history.totalLikes.toString(),
                        )
                    }
                    if (detailedReadingHistory.history.epsCount > 0) {
                        HistoryMetadata(
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            description = "章节",
                            text = "${detailedReadingHistory.history.epsCount}话",
                        )
                    }
                }

                if (onReadLatestClick != null && lastProgress != null && detailedReadingHistory.history.epsCount > lastProgress.chapterNumber) {
                    androidx.compose.material3.Button(
                        onClick = { onReadLatestClick(detailedReadingHistory.history.epsCount) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 10.dp,
                            vertical = 4.dp,
                        ),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "追更最新",
                            style = MaterialTheme.typography.labelSmall,
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
private fun HistoryMetadata(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.outline,
        )
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
private fun ComicHistoryStatusBadges(history: DetailedReadingHistory) {
    val progress = history.lastReadChapterProgress
    if (progress == null && !history.history.finished && !history.history.isFavourited) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (progress != null) {
            val status = when {
                history.history.epsCount > progress.chapterNumber -> "有更新"
                progress.chapterNumber >= history.history.epsCount && progress.isCompleted -> "已读完"
                else -> "已阅读"
            }
            Badge(
                text = status,
                containerColor = when (status) {
                    "有更新" -> androidx.compose.ui.graphics.Color(0xFFFF9800)
                    "已读完" -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    else -> MaterialTheme.colorScheme.secondary
                },
            )
        }
        if (history.history.finished) {
            Badge(
                text = "已完结",
                containerColor = MaterialTheme.colorScheme.primary,
            )
        }
        if (history.history.isFavourited) {
            Badge(
                text = "已收藏",
                containerColor = MaterialTheme.colorScheme.tertiary,
            )
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
        style = MaterialTheme.typography.labelSmall,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        maxLines = 1,
        softWrap = false,
        fontWeight = FontWeight.SemiBold,
        color = androidx.compose.material3.contentColorFor(containerColor),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(containerColor)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}