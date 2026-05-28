package com.shizq.bika.ui.download

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.database.model.DownloadTaskEntity
import com.shizq.bika.sync.workers.DownloadWorker
import com.shizq.bika.utils.TimeUtil
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.sp

data class ComicDownloadGroup(
    val comicId: String,
    val comicTitle: String,
    val coverUrl: String,
    val tasks: List<DownloadTaskEntity>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadListScreen(
    onBackClick: () -> Unit,
    onComicClick: (comicId: String, episodeOrder: Int) -> Unit,
    viewModel: DownloadListViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var currentComicId by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = currentComicId != null) {
        currentComicId = null
    }

    // Group tasks by comicId
    val groupedComics = remember(tasks) {
        tasks.groupBy { it.comicId }.map { (comicId, comicTasks) ->
            val firstTask = comicTasks.first()
            ComicDownloadGroup(
                comicId = comicId,
                comicTitle = firstTask.comicTitle,
                coverUrl = firstTask.coverUrl,
                tasks = comicTasks
            )
        }
    }

    val selectedComic = remember(tasks, currentComicId) {
        if (currentComicId != null) {
            tasks.filter { it.comicId == currentComicId }
                .sortedBy { it.episodeOrder }
        } else {
            emptyList()
        }
    }

    // Auto navigate back to list if all tasks for the selected comic are deleted
    if (currentComicId != null && selectedComic.isEmpty()) {
        currentComicId = null
    }

    val selectedComicTitle = remember(selectedComic) {
        selectedComic.firstOrNull()?.comicTitle ?: "作品下载详情"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (currentComicId == null) "我的下载" else selectedComicTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentComicId == null) {
                                onBackClick()
                            } else {
                                currentComicId = null
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (currentComicId != null) {
                val firstCompletedTask = remember(selectedComic) {
                    selectedComic.filter { it.status == DownloadStatus.COMPLETED }
                        .minByOrNull { it.episodeOrder }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            firstCompletedTask?.let {
                                onComicClick(it.comicId, it.episodeOrder)
                            }
                        },
                        enabled = firstCompletedTask != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = if (firstCompletedTask != null) "开始阅读" else "下载完成后即可阅读",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无下载任务", style = MaterialTheme.typography.bodyLarge)
            }
        } else if (currentComicId == null) {
            // Level 1: Grouped Comics List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groupedComics, key = { it.comicId }) { group ->
                    ComicDownloadGroupItem(
                        group = group,
                        onClick = { currentComicId = group.comicId }
                    )
                }
            }
        } else {
            // Level 2: Detailed Episode List for selected comic
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header Area with Cover and Info
                val firstTask = selectedComic.first()
                val totalPagesAll = selectedComic.sumOf { it.totalPages }
                val downloadedPagesAll = selectedComic.sumOf { it.downloadedPages }
                val completedCount = selectedComic.count { it.status == DownloadStatus.COMPLETED }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(firstTask.coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = firstTask.comicTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "共计 ${selectedComic.size} 个下载章节",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "已完成 $completedCount 个章节 · 已下载共 $downloadedPagesAll/$totalPagesAll 页",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                val failedTasks = remember(selectedComic) {
                    selectedComic.filter { it.status == DownloadStatus.FAILED }
                }
                val hasFailedTasks = failedTasks.isNotEmpty()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            failedTasks.forEach { task ->
                                DownloadWorker.startDownload(
                                    context = context,
                                    comicId = task.comicId,
                                    comicTitle = task.comicTitle,
                                    coverUrl = task.coverUrl,
                                    episodeId = task.episodeId,
                                    episodeTitle = task.episodeTitle,
                                    episodeOrder = task.episodeOrder
                                )
                            }
                        },
                        enabled = hasFailedTasks,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (hasFailedTasks) "重新下载" else "重新下载",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(selectedComic, key = { it.id }) { task ->
                        DownloadTaskItem(
                            task = task,
                            onClick = {
                                if (task.status == DownloadStatus.COMPLETED) {
                                    onComicClick(task.comicId, task.episodeOrder)
                                } else if (task.status == DownloadStatus.FAILED) {
                                    DownloadWorker.startDownload(
                                        context = context,
                                        comicId = task.comicId,
                                        comicTitle = task.comicTitle,
                                        coverUrl = task.coverUrl,
                                        episodeId = task.episodeId,
                                        episodeTitle = task.episodeTitle,
                                        episodeOrder = task.episodeOrder
                                    )
                                }
                            },
                            onDelete = { viewModel.deleteDownload(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComicDownloadGroupItem(
    group: ComicDownloadGroup,
    onClick: () -> Unit
) {
    val completedCount = remember(group.tasks) {
        group.tasks.count { it.status == DownloadStatus.COMPLETED }
    }
    val totalCount = group.tasks.size
    val isDownloading = remember(group.tasks) {
        group.tasks.any { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }
    }
    val averageProgress = remember(group.tasks) {
        val downloadingTasks = group.tasks.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }
        if (downloadingTasks.isNotEmpty()) {
            downloadingTasks.map { it.progress }.average().toInt()
        } else {
            0
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(group.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.comicTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "已下载共 ${totalCount} 个章节",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(
                        text = "已完成 $completedCount",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (isDownloading) {
                        StatusChip(
                            text = "正在下载 ($averageProgress%)",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "详情",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadTaskItem(
    task: DownloadTaskEntity,
    onClick: () -> Unit,
    onDelete: (DownloadTaskEntity) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除下载") },
            text = { Text("确定要删除《${task.comicTitle}》 ${task.episodeTitle} 的下载文件吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(task)
                    showDeleteDialog = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDeleteDialog = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(task.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.episodeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Show page details
                Text(
                    text = "共 ${task.totalPages} 页 (已下载 ${task.downloadedPages} 页)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                when (task.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.PENDING -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { task.progress / 100f },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${task.progress}%",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusChip("已完成", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                                if (task.isViewed) {
                                    StatusChip("已查看", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                            }
                            // Show completion time!
                            task.completedAt?.let { time ->
                                Text(
                                    text = "完成时间: ${TimeUtil().getDate(time.toEpochMilliseconds())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    DownloadStatus.FAILED -> {
                        StatusChip("下载失败 (点击重试)", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusChip(text: String, containerColor: Color, contentColor: Color) {
    SuggestionChip(
        onClick = { },
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
            labelColor = contentColor
        ),
        border = null,
        modifier = Modifier.height(24.dp)
    )
}
