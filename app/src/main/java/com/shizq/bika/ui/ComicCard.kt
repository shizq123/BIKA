package com.shizq.bika.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
    onClick: () -> Unit = {},
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 漫画封面 (尺寸已增大)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(history.cover)
                    .crossfade(true)
                    .build(),
                contentDescription = "${history.title} Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(3f / 4f)  // <--- 这里是主要修改
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 右侧信息区域 (为了容纳更大的图片，高度也需要足够)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp), // 让Column高度与图片匹配，确保内容垂直居中
                verticalArrangement = Arrangement.SpaceBetween // 使用SpaceBetween让元素上下分布
            ) {
                // 标题和作者
                Column {
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
                }


                // 进度和时间
                Column {
                    // 阅读进度文本和进度条
                    val progressText = "读至: 第${history.lastReadProgress.chapterIndex + 1}章"
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val progress = remember(history) {
                        val maxPage = history.maxPage
                        if (maxPage != null && maxPage > 0) {
                            (history.lastReadProgress.chapterIndex + 1).toFloat() / maxPage
                        } else {
                            0f
                        }
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

                    // 最后阅读时间
                    Text(
                        text = formatLastReadTime(history.lastReadAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
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
        cover = "https://img.picacomic.com/Rc9IZBGmXzl4__r36cUghtHByOPzZKecUKMKPtW21Ys/rs:fill:300:400:0/g:sm/aHR0cHM6Ly9zdG9yYWdlLWIucGljYWNvbWljLmNvbS9zdGF0aWMvZDQ4YmM5MDAtYTIxYi00ZDJhLTk1MDctN2JmMzRhNWZjNDM5LmpwZw.jpg",
        lastReadAt = Clock.System.now() - 5.hours,
        lastReadProgress = ReadingProgress(chapterIndex = 10, pageIndex = 5),
        readChapters = emptySet(),
        maxPage = 208
    )

    Box(modifier = Modifier.padding(16.dp)) {
        ComicCard(history = sampleHistory, onClick = {})
    }
}