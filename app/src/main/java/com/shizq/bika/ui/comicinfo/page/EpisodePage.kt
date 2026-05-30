package com.shizq.bika.ui.comicinfo.page

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.shizq.bika.core.network.model.Episode
import kotlinx.coroutines.flow.flowOf
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.shizq.bika.core.database.model.DownloadTaskEntity
import com.shizq.bika.core.database.model.DownloadStatus

@Composable
fun EpisodesPage(
    episodes: LazyPagingItems<Episode>,
    modifier: Modifier = Modifier,
    downloadTasks: List<DownloadTaskEntity> = emptyList(),
    navigateToReader: (index: Int) -> Unit = { _ -> },
    onDownloadClick: (Episode) -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 96.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                count = episodes.itemCount,
                key = episodes.itemKey { it.id }
            ) { index ->
                episodes[index]?.let { episode ->
                    val task = downloadTasks.find { it.episodeId == episode.id }
                    EpisodeItem(
                        text = episode.title,
                        downloadStatus = task?.status,
                        onClick = {
                            navigateToReader(episode.order)
                        },
                        onDownload = {
                            onDownloadClick(episode)
                        }
                    )
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
    downloadStatus: DownloadStatus? = null,
    onDownload: () -> Unit = {}
) {
    androidx.compose.material3.OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            when (downloadStatus) {
                DownloadStatus.COMPLETED -> {
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "已下载",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
                DownloadStatus.DOWNLOADING, DownloadStatus.PENDING -> {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                else -> {
                    IconButton(onClick = onDownload) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "下载",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
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