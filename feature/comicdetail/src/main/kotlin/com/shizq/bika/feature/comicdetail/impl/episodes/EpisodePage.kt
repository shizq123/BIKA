package com.shizq.bika.feature.comicdetail.impl.episodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.database.model.ChapterProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 目录分页的生命周期边界。
 *
 * 上层只传递冷 Flow；只有该 Route 真正进入组合时才创建 LazyPagingItems，
 * 避免详情页初次打开就启动章节分页。
 */
@Composable
fun EpisodesRoute(
    episodesFlow: Flow<PagingData<Chapter>>,
    chapterProgress: List<ChapterProgressEntity>,
    navigateToReader: (chapterOrder: Int) -> Unit,
    onDownloadSelectionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val episodes = episodesFlow.collectAsLazyPagingItems()
    EpisodesPage(
        episodes = episodes,
        modifier = modifier,
        chapterProgress = chapterProgress,
        navigateToReader = navigateToReader,
        onDownloadSelectionClick = onDownloadSelectionClick,
    )
}

@Composable
fun EpisodesPage(
    episodes: LazyPagingItems<Chapter>,
    modifier: Modifier = Modifier,
    chapterProgress: List<ChapterProgressEntity> = emptyList(),
    navigateToReader: (chapterOrder: Int) -> Unit = {},
    onDownloadSelectionClick: () -> Unit = {},
) {
    val progressByOrder = remember(chapterProgress) {
        chapterProgress.associateBy { it.chapterId }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "章节目录",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (episodes.itemCount > 0) {
                        "共 ${episodes.itemCount} 话"
                    } else {
                        "正在加载章节"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = onDownloadSelectionClick) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                )
                Text("选择下载", modifier = Modifier.padding(start = 8.dp))
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 4.dp,
                end = 16.dp,
                bottom = 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(
                count = episodes.itemCount,
                key = { index ->
                    episodes.peek(index)?.id ?: "placeholder_$index"
                },
            ) { index ->
                episodes[index]?.let { episode ->
                    EpisodeItem(
                        text = episode.title,
                        progress = progressByOrder[episode.order],
                        onClick = { navigateToReader(episode.order) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Episodes Page Preview")
@Composable
private fun EpisodesPagePreview() {
    val fakeEpisodes = List(20) { index ->
        Chapter(
            id = index.toString(),
            title = "第 ${index + 1} 话",
            order = index + 1,
            updatedAt = "",
        )
    }
    val episodes = flowOf(PagingData.from(fakeEpisodes)).collectAsLazyPagingItems()

    MaterialTheme {
        EpisodesPage(episodes = episodes)
    }
}
