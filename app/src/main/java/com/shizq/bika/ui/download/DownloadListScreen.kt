package com.shizq.bika.ui.download

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.shizq.bika.core.database.model.DownloadStatus
import com.shizq.bika.core.download.model.DownloadTask
import com.shizq.bika.utils.TimeUtil

@Composable
fun DownloadListScreen(
    onBackClick: () -> Unit,
    onComicClick: (comicId: String, episodeOrder: Int) -> Unit,
    viewModel: DownloadListViewModel = hiltViewModel()
) {
    val navState by viewModel.navState.collectAsStateWithLifecycle()
    val groupedComics by viewModel.groupedComics.collectAsStateWithLifecycle()
    val selectedComicEpisodes by viewModel.selectedComicEpisodes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentComicId = (navState as? DownloadNavState.ComicDetail)?.comicId
    val isInDetail = currentComicId != null
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedEpisodeOrders = remember { mutableStateListOf<Int>() }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    val exitSelectMode = {
        isSelectMode = false
        selectedEpisodeOrders.clear()
        showBatchDeleteDialog = false
    }
    val selectedComicTitle = selectedComicEpisodes.firstOrNull()?.task?.comicTitle ?: "作品下载详情"
    val allCompletedTasks = selectedComicEpisodes
        .map { it.task }
        .filter { it.status == DownloadStatus.COMPLETED }
    val selectedOrderSet = selectedEpisodeOrders.toSet()
    val selectedCompletedTasks = allCompletedTasks.filter { it.episodeOrder in selectedOrderSet }
    val firstCompletedTask = allCompletedTasks.minByOrNull { it.episodeOrder }
    val failedTasks = selectedComicEpisodes
        .map { it.task }
        .filter { it.status == DownloadStatus.FAILED }
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = getFileName(context, uri) ?: "imported_comic.cbz"
            if (!fileName.endsWith(".zip", ignoreCase = true) &&
                !fileName.endsWith(".cbz", ignoreCase = true)
            ) {
                android.widget.Toast.makeText(
                    context,
                    "仅支持导入 .cbz 或 .zip 格式的漫画文件",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@rememberLauncherForActivityResult
            }
            viewModel.importCbz(uri = uri, fileName = fileName)
        }
    }
    // 切换漫画/返回一级时清理选择状态
    LaunchedEffect(currentComicId) {
        exitSelectMode()
    }
    // 保证多选状态始终只保留“当前详情页里仍然存在且已完成”的章节
    LaunchedEffect(selectedComicEpisodes, isSelectMode) {
        val validCompletedOrders = selectedComicEpisodes
            .filter { it.task.status == DownloadStatus.COMPLETED }
            .map { it.task.episodeOrder }
            .toSet()
        selectedEpisodeOrders.retainAll(validCompletedOrders)
        if (isSelectMode && validCompletedOrders.isEmpty()) {
            exitSelectMode()
        }
    }
    BackHandler(enabled = isInDetail || isSelectMode) {
        if (isSelectMode) {
            exitSelectMode()
        } else if (isInDetail) {
            viewModel.navigateBack()
        }
    }
    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("批量删除") },
            text = { Text("确定要删除选中的 ${selectedCompletedTasks.size} 个章节吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMultipleDownloads(selectedCompletedTasks)
                        exitSelectMode()
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            isSelectMode -> "已选择 ${selectedCompletedTasks.size} 项"
                            !isInDetail -> "我的下载"
                            else -> selectedComicTitle
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when {
                                isSelectMode -> exitSelectMode()
                                isInDetail -> viewModel.navigateBack()
                                else -> onBackClick()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelectMode) {
                                Icons.Default.Close
                            } else {
                                Icons.AutoMirrored.Rounded.ArrowBack
                            },
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!isInDetail) {
                        IconButton(onClick = { pickerLauncher.launch("*/*") }) {
                            Icon(Icons.Default.Add, contentDescription = "导入本地漫画")
                        }
                    } else {
                        if (isSelectMode) {
                            val isAllSelected =
                                allCompletedTasks.isNotEmpty() &&
                                        selectedCompletedTasks.size == allCompletedTasks.size
                            IconButton(
                                onClick = {
                                    if (isAllSelected) {
                                        selectedEpisodeOrders.clear()
                                    } else {
                                        selectedEpisodeOrders.clear()
                                        selectedEpisodeOrders.addAll(
                                            allCompletedTasks.map { it.episodeOrder }
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Default.SelectAll, contentDescription = "全选")
                            }
                        } else {
                            IconButton(
                                onClick = { isSelectMode = true },
                                enabled = allCompletedTasks.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "多选")
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isInDetail) {
                if (isSelectMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { showBatchDeleteDialog = true },
                            enabled = selectedCompletedTasks.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text(
                                "删除选中 (${selectedCompletedTasks.size})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.exportMultipleToZip(
                                    tasks = selectedCompletedTasks,
                                    comicTitle = selectedComicTitle
                                )
                                exitSelectMode()
                            },
                            enabled = selectedCompletedTasks.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("打包归档", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
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
        }
    ) { paddingValues ->
        when {
            !isInDetail && groupedComics.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无下载任务", style = MaterialTheme.typography.bodyLarge)
                }
            }

            !isInDetail -> {
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
                            onClick = { viewModel.navigateToDetail(group.comicId) }
                        )
                    }
                }
            }

            else -> {
                val firstTask = selectedComicEpisodes.firstOrNull()?.task
                if (firstTask == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无下载章节", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    val totalPagesAll = selectedComicEpisodes.sumOf { it.task.totalPages }
                    val downloadedPagesAll = selectedComicEpisodes.sumOf { it.task.downloadedPages }
                    val completedCount =
                        selectedComicEpisodes.count { it.task.status == DownloadStatus.COMPLETED }
                    val hasFailedTasks = failedTasks.isNotEmpty()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
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
                                        text = "共计 ${selectedComicEpisodes.size} 个下载章节",
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { failedTasks.forEach(viewModel::retryDownload) },
                                enabled = hasFailedTasks,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (hasFailedTasks) {
                                        "重新下载失败章节 (${failedTasks.size})"
                                    } else {
                                        "暂无失败章节"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(selectedComicEpisodes, key = { it.task.id }) { taskWithProgress ->
                                val task = taskWithProgress.task
                                val isSelected = task.episodeOrder in selectedOrderSet
                                DownloadTaskItem(
                                    taskWithProgress = taskWithProgress,
                                    onClick = {
                                        if (isSelectMode) {
                                            if (task.status == DownloadStatus.COMPLETED) {
                                                if (isSelected) {
                                                    selectedEpisodeOrders.remove(task.episodeOrder)
                                                } else {
                                                    selectedEpisodeOrders.add(task.episodeOrder)
                                                }
                                            }
                                        } else {
                                            when (task.status) {
                                                DownloadStatus.COMPLETED -> {
                                                    onComicClick(task.comicId, task.episodeOrder)
                                                }

                                                DownloadStatus.FAILED -> {
                                                    viewModel.retryDownload(task)
                                                }

                                                else -> Unit
                                            }
                                        }
                                    },
                                    onDelete = viewModel::deleteDownload,
                                    onBringToTop = viewModel::bringToTop,
                                    onExport = viewModel::exportToCbz,
                                    isSelectMode = isSelectMode,
                                    isSelected = isSelected,
                                    onLongClick = {
                                        if (!isSelectMode && task.status == DownloadStatus.COMPLETED) {
                                            isSelectMode = true
                                            if (task.episodeOrder !in selectedEpisodeOrders) {
                                                selectedEpisodeOrders.add(task.episodeOrder)
                                            }
                                        }
                                    }
                                )
                            }
                        }
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
        group.tasks.any {
            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING
        }
    }
    val averageProgress = remember(group.tasks) {
        val downloadingTasks = group.tasks.filter {
            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING
        }
        if (downloadingTasks.isNotEmpty()) {
            downloadingTasks.map { it.progress }.average().toInt()
        } else {
            0
        }
    }
    val failedCount = remember(group.tasks) {
        group.tasks.count { it.status == DownloadStatus.FAILED }
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
                    text = "已下载共 $totalCount 个章节",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    if (failedCount > 0) {
                        StatusChip(
                            text = "下载失败 $failedCount",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    group.readSummary?.let { summary ->
                        if (summary.finishedCount > 0) {
                            StatusChip(
                                text = "已读完 ${summary.finishedCount}",
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            )
                        }
                        if (summary.readingCount > 0) {
                            StatusChip(
                                text = "阅读中 ${summary.readingCount}",
                                containerColor = Color(0xFFFF9800),
                                contentColor = Color.White
                            )
                        }
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DownloadTaskItem(
    taskWithProgress: DownloadTaskWithProgress,
    onClick: () -> Unit,
    onDelete: (DownloadTask) -> Unit,
    onBringToTop: (DownloadTask) -> Unit,
    onExport: (DownloadTask) -> Unit,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onLongClick: () -> Unit
) {
    val task = taskWithProgress.task
    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除下载") },
            text = { Text("确定要删除《${task.comicTitle}》 ${task.episodeTitle} 的下载文件吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(task)
                        showDeleteDialog = false
                    }
                ) {
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
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp),
                    enabled = task.status == DownloadStatus.COMPLETED
                )
            }
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.episodeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                StatusChip(
                                    text = "已完成",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                when {
                                    taskWithProgress.isFinished -> {
                                        StatusChip(
                                            text = "已读完",
                                            containerColor = Color(0xFF4CAF50),
                                            contentColor = Color.White
                                        )
                                    }
                                    taskWithProgress.isRead -> {
                                        val progress = taskWithProgress.readProgress!!
                                        StatusChip(
                                            text = "阅读中 ${progress.currentPage}/${progress.pageCount}页",
                                            containerColor = Color(0xFFFF9800),
                                            contentColor = Color.White
                                        )
                                    }
                                }
                            }
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
                        StatusChip(
                            text = "下载失败 (点击重试)",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    else -> {}
                }
            }
            if (!isSelectMode) {
                if (task.status != DownloadStatus.COMPLETED) {
                    IconButton(onClick = { onBringToTop(task) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "置顶优先",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (task.status == DownloadStatus.COMPLETED) {
                    IconButton(onClick = { onExport(task) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "导出章节",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
}
@Composable
fun StatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    SuggestionChip(
        onClick = { },
        enabled = false,
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
            labelColor = contentColor
        ),
        border = null,
        modifier = Modifier.height(24.dp)
    )
}
fun getFileName(context: android.content.Context, uri: android.net.Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}