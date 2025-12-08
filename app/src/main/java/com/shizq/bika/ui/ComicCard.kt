package com.shizq.bika.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.shizq.bika.core.data.model.History
import com.shizq.bika.core.data.model.ReadingProgress
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@Composable
fun ComicCard(
    history: History,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(history.cover)
                    .crossfade(true)
                    .build(),
                contentDescription = "${history.title} Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 80.dp, height = 110.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 右侧信息区域
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = history.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = history.author,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                val progressText = "读至: 第${history.lastReadProgress.chapterIndex + 1}话"
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 进度条
                val progress = remember(history) {
                    history.maxPage?.let {
                        if (it > 0) {
                            (history.lastReadProgress.chapterIndex + 1).toFloat() / it
                        } else 0f
                    } ?: 0f
                }

                if (progress > 0) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        strokeCap = StrokeCap.Round,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatLastReadTime(history.lastReadAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * 将 Instant 格式化为易于阅读的相对时间字符串
 */
@Composable
private fun formatLastReadTime(instant: Instant): String {
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


@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun ComicCardPreview() {
    val sampleHistory = History(
        id = "1",
        title = "鬼灭之刃",
        author = "吾峠呼世晴",
        cover = "https://example.com/cover.jpg",
        lastReadAt = Clock.System.now() - 5.hours,
        lastReadProgress = ReadingProgress(chapterIndex = 10, pageIndex = 5),
        readChapters = emptySet(),
        maxPage = 208
    )

    Box(modifier = Modifier.padding(16.dp)) {
        ComicCard(history = sampleHistory, onItemClick = {})
    }
}