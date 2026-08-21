package com.shizq.bika.feature.reader.impl.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.shizq.bika.paging.Chapter

@Composable
fun ChapterList(
    chapters: LazyPagingItems<Chapter>,
    currentChapterOrder: Int,
    onChapterClick: (Chapter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        item {
            Text(
                text = "章节列表",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider()
        }

        items(
            count = chapters.itemCount,
            key = { index ->
                // 与阅读页一致：index+id 组合 key，避免服务端返回重复 id 导致崩溃
                val chapter = chapters.peek(index)
                if (chapter != null) "${index}_${chapter.id}" else "placeholder_$index"
            }
        ) { index ->
            chapters[index]?.let { chapter ->
                val isCurrent = chapter.order == currentChapterOrder
                ChapterListItem(
                    chapter = chapter,
                    isCurrent = isCurrent,
                    onClick = { onChapterClick(chapter) },
                )
            }
        }
    }
}

@Composable
fun ChapterListItem(
    chapter: Chapter,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }

    val titleColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = chapter.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isCurrent) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Playing", tint = titleColor)
        }
    }
}