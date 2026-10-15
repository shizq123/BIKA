package com.shizq.bika.feature.comicdetail.impl.episodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
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
 * 监听 Paging 状态，提供自动重试、单话兼容与完整状态机展示。
 */
@Composable
fun EpisodesRoute(
    episodesFlow: Flow<PagingData<Chapter>>,
    chapterProgress: List<ChapterProgressEntity>,
    navigateToReader: (chapterOrder: Int) -> Unit,
    onDownloadSelectionClick: () -> Unit,
    modifier: Modifier = Modifier,
    epsCount: Int = 0,
) {
    val episodes = episodesFlow.collectAsLazyPagingItems()

    // 若因滑动或切换导致初次中断出错，自动静默触发重试
    LaunchedEffect(episodes.loadState.refresh) {
        if (episodes.loadState.refresh is LoadState.Error && episodes.itemCount == 0) {
            episodes.retry()
        }
    }

    EpisodesPage(
        episodes = episodes,
        modifier = modifier,
        chapterProgress = chapterProgress,
        navigateToReader = navigateToReader,
        onDownloadSelectionClick = onDownloadSelectionClick,
        epsCount = epsCount,
    )
}

@Composable
fun EpisodesPage(
    episodes: LazyPagingItems<Chapter>,
    modifier: Modifier = Modifier,
    chapterProgress: List<ChapterProgressEntity> = emptyList(),
    navigateToReader: (chapterOrder: Int) -> Unit = {},
    onDownloadSelectionClick: () -> Unit = {},
    epsCount: Int = 0,
) {
    val progressByOrder = remember(chapterProgress) {
        chapterProgress.associateBy { it.chapterId }
    }

    val refreshState = episodes.loadState.refresh
    val isRefreshing = refreshState is LoadState.Loading
    val isError = refreshState is LoadState.Error
    val isSingleEpisode = epsCount <= 1 && episodes.itemCount == 0 && !isRefreshing

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
                    text = when {
                        episodes.itemCount > 0 -> "共 ${episodes.itemCount} 话"
                        isSingleEpisode -> "全一话"
                        isRefreshing -> "正在加载章节..."
                        isError -> "加载失败"
                        else -> "共 0 话"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
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

        when {
            // 1. 正在加载且暂无缓存数据
            isRefreshing && episodes.itemCount == 0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "正在获取章节列表...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 2. 加载失败且无数据，展示友好错误态与重试按钮
            isError && episodes.itemCount == 0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(
                            text = "章节目录加载失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "网络连接异常或已被中断",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { episodes.retry() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text("点击重试", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }

            // 3. 单话漫画（服务端未返回独立 eps 列表时合成单话呈现）
            isSingleEpisode -> {
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
                    item {
                        EpisodeItem(
                            text = "全一话",
                            progress = progressByOrder[1],
                            onClick = { navigateToReader(1) },
                        )
                    }
                }
            }

            // 4. 正常多话章节网格列表
            episodes.itemCount > 0 -> {
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

            // 5. 空列表态
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "暂无章节目录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { episodes.refresh() }) {
                            Text("刷新")
                        }
                    }
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
