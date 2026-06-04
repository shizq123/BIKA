package com.shizq.bika.ui.comicinfo.page

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.shizq.bika.core.database.model.ChapterProgressEntity
import com.shizq.bika.core.database.model.DownloadTaskEntity
import com.shizq.bika.core.network.model.Episode
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodesPage(
    episodes: LazyPagingItems<Episode>,
    modifier: Modifier = Modifier,
    downloadTasks: List<DownloadTaskEntity> = emptyList(),
    chapterProgress: List<ChapterProgressEntity> = emptyList(),
    navigateToReader: (index: Int) -> Unit = { _ -> },
    onDownloadClick: (List<Episode>) -> Unit = {},
    onFetchAllEpisodes: suspend () -> List<Episode> = { emptyList() }
) {
    var showDownloadSelectSheet by remember { mutableStateOf(false) }
    var selectedEpisodeIds by remember { mutableStateOf(setOf<String>()) }
    var allEpisodes by remember { mutableStateOf<List<Episode>?>(null) }
    var isLoadingEpisodes by remember { mutableStateOf(false) }

    LaunchedEffect(showDownloadSelectSheet) {
        if (showDownloadSelectSheet && allEpisodes == null) {
            isLoadingEpisodes = true
            try {
                allEpisodes = onFetchAllEpisodes()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingEpisodes = false
            }
        }
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
                key = episodes.itemKey { it.id }
            ) { index ->
                episodes[index]?.let { episode ->
                    val progress = chapterProgress.find { it.chapterId == episode.order }
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
            onClick = { showDownloadSelectSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "下载选择"
            )
        }
    }

    if (showDownloadSelectSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showDownloadSelectSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择要下载的章节",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isLoadingEpisodes && !allEpisodes.isNullOrEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    selectedEpisodeIds = allEpisodes?.map { it.id }?.toSet() ?: emptySet()
                                }
                            ) {
                                Text("全选")
                            }
                            TextButton(
                                onClick = {
                                    selectedEpisodeIds = emptySet()
                                }
                            ) {
                                Text("清除")
                            }
                        }
                    }
                }

                if (isLoadingEpisodes) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (allEpisodes.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("未找到可选章节", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val currentEpisodes = allEpisodes!!
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(currentEpisodes.size) { index ->
                            val episode = currentEpisodes[index]
                            val isSelected = episode.id in selectedEpisodeIds

                            Surface(
                                onClick = {
                                    selectedEpisodeIds = if (isSelected) {
                                        selectedEpisodeIds - episode.id
                                    } else {
                                        selectedEpisodeIds + episode.id
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = episode.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val selectedList = currentEpisodes.filter { it.id in selectedEpisodeIds }
                            onDownloadClick(selectedList)
                            showDownloadSelectSheet = false
                            selectedEpisodeIds = emptySet()
                        },
                        enabled = selectedEpisodeIds.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("开始下载所选章节 (${selectedEpisodeIds.size})")
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: ChapterProgressEntity? = null
) {
    androidx.compose.material3.OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (progress != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
        if (progress != null) {
            val isCompleted = progress.pageCount > 0 && progress.currentPage >= progress.pageCount
            val progressText = if (isCompleted) "已读完" else "看到第 ${progress.currentPage} 页"
            val pageText = if (progress.pageCount > 0) "共 ${progress.pageCount} 页" else "页数未知"
            Text(
                text = "$pageText · $progressText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = "未阅读",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    
    if (progress != null) {
        val isCompleted = progress.pageCount > 0 && progress.currentPage >= progress.pageCount
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isCompleted) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isCompleted) 
                MaterialTheme.colorScheme.onPrimaryContainer 
            else 
                MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Text(
                text = if (isCompleted) "已读完" else "已看",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true, name = "Episodes Page Preview")
@Composable
private fun EpisodesPagePreview() {
    val fakeEpisodes = List(20) { i ->
        Episode(
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

@Preview(showBackground = true, name = "Episode Item Preview")
@Composable
private fun EpisodeItemPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            EpisodeItem(
                text = "第 01 话",
                onClick = {}
            )
        }
    }
}