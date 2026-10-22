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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withTimeoutOrNull

/** 首屏加载失败时的静默重试次数上限，超过后把错误态交给用户。 */
private const val MAX_SILENT_RETRIES = 2

/** 静默重试的基准间隔，实际等待时间按重试次数线性退避。 */
private const val SILENT_RETRY_DELAY_MILLIS = 800L

/** 首屏一直 Loading 且一页数据都没有，超过这个时长就判定为卡死。 */
private const val STALL_TIMEOUT_MILLIS = 20_000L

/**
 * 目录分页的生命周期边界。
 *
 * 监听 Paging 状态，提供自动重试、加载超时自救、单话兼容与完整状态机展示。
 *
 * 之所以要「超时自救」：分页对象在订阅被中断（例如进入阅读器、详情页离开组合）后
 * 可能不再产出任何事件，而 refresh 的 LoadState 仍停在 Loading。此时只靠
 * `LoadState` 分支渲染的界面会一直转圈，用户没有出口；超时后改成错误态，
 * 由 [onReloadEpisodes] 换一代分页重新请求。
 */
@Composable
fun EpisodesRoute(
    episodesFlow: Flow<PagingData<Chapter>>,
    chapterProgress: List<ChapterProgressEntity>,
    navigateToReader: (chapterOrder: Int) -> Unit,
    onDownloadSelectionClick: () -> Unit,
    onReloadEpisodes: () -> Unit = {},
    modifier: Modifier = Modifier,
    epsCount: Int = 0,
) {
    val episodes = episodesFlow.collectAsLazyPagingItems()

    var silentRetries by remember(episodesFlow) { mutableIntStateOf(0) }
    var isStalled by remember(episodesFlow) { mutableStateOf(false) }

    LaunchedEffect(episodesFlow, episodes.loadState.refresh, episodes.itemCount) {
        // 已经有数据时不再拦截用户：刷新成功与否都让列表继续展示。
        if (episodes.itemCount > 0) {
            silentRetries = 0
            isStalled = false
            return@LaunchedEffect
        }
        when (episodes.loadState.refresh) {
            // 失败且没有任何数据：静默补几次，避免「滑动/切换」造成的首次中断把页面留在错误态。
            // 次数必须有上限——无限重试会让 UI 在 Loading 与 Error 之间来回跳，
            // 用户既看不到错误提示，也等不到一个可以点的重试按钮。
            is LoadState.Error -> {
                if (silentRetries < MAX_SILENT_RETRIES) {
                    silentRetries++
                    delay(SILENT_RETRY_DELAY_MILLIS * silentRetries)
                    episodes.retry()
                }
            }
            // 一直 Loading 且一页都没回来：分页对象可能已经不再产出事件，
            // 转圈会一直转下去，超时后交给用户决定是否重新加载。
            is LoadState.Loading -> {
                isStalled = withTimeoutOrNull(STALL_TIMEOUT_MILLIS) {
                    snapshotFlow {
                        episodes.loadState.refresh !is LoadState.Loading || episodes.itemCount > 0
                    }.first()
                    false
                } ?: true
            }
            else -> isStalled = false
        }
    }

    EpisodesPage(
        episodes = episodes,
        modifier = modifier,
        chapterProgress = chapterProgress,
        navigateToReader = navigateToReader,
        onDownloadSelectionClick = onDownloadSelectionClick,
        onReloadEpisodes = onReloadEpisodes,
        epsCount = epsCount,
        isStalled = isStalled,
    )
}

@Composable
fun EpisodesPage(
    episodes: LazyPagingItems<Chapter>,
    modifier: Modifier = Modifier,
    chapterProgress: List<ChapterProgressEntity> = emptyList(),
    navigateToReader: (chapterOrder: Int) -> Unit = {},
    onDownloadSelectionClick: () -> Unit = {},
    onReloadEpisodes: () -> Unit = {},
    epsCount: Int = 0,
    isStalled: Boolean = false,
) {
    val progressByOrder = remember(chapterProgress) {
        chapterProgress.associateBy { it.chapterId }
    }

    val refreshState = episodes.loadState.refresh
    val isRefreshing = refreshState is LoadState.Loading
    val isError = refreshState is LoadState.Error
    val isSingleEpisode = epsCount <= 1 && episodes.itemCount == 0 && !isRefreshing && !isStalled
    val hasFailed = isError || isStalled

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
                        isStalled -> "加载超时"
                        isError -> "加载失败"
                        isRefreshing -> "正在加载章节..."
                        else -> "共 0 话"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
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
            // 1. 正在加载且暂无缓存数据（超时后不再无条件转圈）
            isRefreshing && episodes.itemCount == 0 && !isStalled -> {
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

            // 2. 加载失败 / 加载超时且无数据，展示友好错误态与重新加载入口
            hasFailed && episodes.itemCount == 0 -> {
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
                            text = if (isStalled) "章节目录加载超时" else "章节目录加载失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isStalled) "长时间没有收到章节列表，请重新加载" else "网络连接异常或已被中断",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onReloadEpisodes) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text(
                                text = if (isStalled) "重新加载" else "点击重试",
                                modifier = Modifier.padding(start = 6.dp),
                            )
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
                        OutlinedButton(onClick = onReloadEpisodes) {
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
