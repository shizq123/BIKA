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
            modifier = Modifier.defaultMinSize(minHeight = 135.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    Text(
                        text = detailedReadingHistory.history.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = detailedReadingHistory.history.author,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val lastProgress = detailedReadingHistory.lastReadChapterProgress

                // 只有在有阅读进度时才显示进度部分
                if (lastProgress != null) {
                    Column {
                        val progressText =
                            "读至: 第${lastProgress.chapterIndex}话 ${lastProgress.currentPage}/${lastProgress.pageCount}页"
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

                // 最后交互时间
                Text(
                    text = formatRelativeTime(detailedReadingHistory.history.lastInteractionAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
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