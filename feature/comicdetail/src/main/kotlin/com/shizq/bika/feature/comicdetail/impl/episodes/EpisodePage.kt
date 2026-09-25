package com.shizq.bika.feature.comicdetail.impl.episodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.shizq.bika.core.data.model.Chapter
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.feature.comicdetail.impl.R
import kotlinx.coroutines.flow.flowOf

@Composable
fun EpisodesPage(
    episodes: LazyPagingItems<Chapter>,
    modifier: Modifier = Modifier,
    chapterProgress: List<ChapterProgressEntity> = emptyList(),
    navigateToReader: (index: Int) -> Unit = { _ -> },
    onDownloadSelectionClick: () -> Unit = {},
) {

    // 原先每个 item 都对整个列表做一次 find，O(n) × item 数
    val progressByOrder = remember(chapterProgress) {
        chapterProgress.associateBy { it.chapterId }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 96.dp // 增加底部边距，防止最后一个卡片被右下角的 FloatingActionButton 遮挡
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = episodes.itemCount,
                // key 只用 id：跨页重复已在 EpisodePagingSource 内去重。
                // 掺入 index 会让新数据插入后所有后续 item 的身份发生变化。
                key = { index ->
                    val episode = episodes.peek(index)
                    if (episode != null) episode.id else "placeholder_$index"
                }
            ) { index ->
                episodes[index]?.let { episode ->
                    val progress = progressByOrder[episode.order]
                    EpisodeItem(
                        text = episode.title,
                        progress = progress,
                        onClick = {
                            navigateToReader(episode.order)
                        }
                    )
                }
            }
        }

        // 右下角新增下载选择悬浮按钮
        FloatingActionButton(
            onClick = onDownloadSelectionClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = stringResource(R.string.download_select_action)
            )
        }
    }

}
@Preview(showBackground = true, name = "Episodes Page Preview")
@Composable
private fun EpisodesPagePreview() {
    val fakeEpisodes = List(20) { i ->
        Chapter(
            id = i.toString(),
            title = "第 ${i + 1} 话",
            order = i + 1,
            updatedAt = ""
        )
    }
    val pagingDataFlow = flowOf(PagingData.from(fakeEpisodes))
    val lazyPagingItems = pagingDataFlow.collectAsLazyPagingItems()

    MaterialTheme {
        EpisodesPage(episodes = lazyPagingItems)
    }
}