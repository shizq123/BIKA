package com.shizq.bika.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.shizq.bika.core.database.model.DownloadTaskEntity
import com.shizq.bika.core.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import javax.inject.Inject

data class OfflineComicItem(
    val comicId: String,
    val title: String,
    val coverUrl: String,
    val downloadedEpisodesCount: Int,
    val sizeInBytes: Long,
    val tasks: List<DownloadTaskEntity>
)

data class StorageState(
    val coilCacheSize: Long = 0,
    val downloadsSize: Long = 0,
    val offlineComics: List<OfflineComicItem> = emptyList(),
    val isClearing: Boolean = false
)

@HiltViewModel
class StorageManagerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StorageState())
    val state: StateFlow<StorageState> = _state

    init {
        loadStorageInfo()
    }

    fun loadStorageInfo() {
        viewModelScope.launch {
            val coilSize = getCoilCacheSize()
            val downloadsInfo = getDownloadsInfo()
            _state.value = StorageState(
                coilCacheSize = coilSize,
                downloadsSize = downloadsInfo.first,
                offlineComics = downloadsInfo.second
            )
        }
    }

    private suspend fun getCoilCacheSize(): Long = withContext(Dispatchers.IO) {
        val imageLoader = SingletonImageLoader.get(context)
        imageLoader.diskCache?.size ?: 0L
    }

    private suspend fun getDownloadsInfo(): Pair<Long, List<OfflineComicItem>> = withContext(Dispatchers.IO) {
        val tasks = downloadRepository.getAllTasks().first()
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val comicsDir = File(base, ".bika/comics")
        
        var totalSize = 0L
        val comicsList = mutableListOf<OfflineComicItem>()

        if (comicsDir.exists() && comicsDir.isDirectory) {
            val comicFolders = comicsDir.listFiles() ?: emptyArray()
            val tasksGrouped = tasks.groupBy { it.comicId }

            for (folder in comicFolders) {
                if (folder.isDirectory) {
                    val comicId = folder.name
                    val size = getFolderSize(folder)
                    totalSize += size

                    val comicTasks = tasksGrouped[comicId] ?: emptyList()
                    val title = comicTasks.firstOrNull()?.comicTitle ?: "未知漫画"
                    val coverUrl = comicTasks.firstOrNull()?.coverUrl ?: ""

                    comicsList.add(
                        OfflineComicItem(
                            comicId = comicId,
                            title = title,
                            coverUrl = coverUrl,
                            downloadedEpisodesCount = comicTasks.size,
                            sizeInBytes = size,
                            tasks = comicTasks
                        )
                    )
                }
            }
        }
        
        Pair(totalSize, comicsList.sortedByDescending { it.sizeInBytes })
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        val files = file.listFiles()
        if (files != null) {
            for (f in files) {
                size += getFolderSize(f)
            }
        }
        return size
    }

    fun clearCoilCache() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isClearing = true)
            withContext(Dispatchers.IO) {
                val imageLoader = SingletonImageLoader.get(context)
                imageLoader.diskCache?.clear()
                imageLoader.memoryCache?.clear()
            }
            _state.value = _state.value.copy(
                coilCacheSize = 0L,
                isClearing = false
            )
        }
    }

    fun deleteComicDownloads(comicId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isClearing = true)

            val currentList = _state.value.offlineComics
            val targetItem = currentList.find { it.comicId == comicId }
            val targetSize = targetItem?.sizeInBytes ?: 0L

            withContext(Dispatchers.IO) {
                val base = context.getExternalFilesDir(null) ?: context.filesDir
                val folder = File(base, ".bika/comics/$comicId")
                if (folder.exists()) {
                    folder.deleteRecursively()
                }

                val tasks = downloadRepository.getTasksByComic(comicId).first()
                downloadRepository.deleteDownloads(tasks)
            }

            val updatedList = currentList.filter { it.comicId != comicId }
            val newDownloadsSize = (_state.value.downloadsSize - targetSize).coerceAtLeast(0L)

            _state.value = _state.value.copy(
                downloadsSize = newDownloadsSize,
                offlineComics = updatedList,
                isClearing = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagerScreen(
    onBackClick: () -> Unit,
    viewModel: StorageManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("存储与空间管理") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.isClearing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // 头部总览卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "磁盘空间分析",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "图片缓存 (Coil)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatSize(state.coilCacheSize),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = { viewModel.clearCoilCache() },
                            enabled = !state.isClearing && state.coilCacheSize > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("清空缓存")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "离线漫画内容",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatSize(state.downloadsSize),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Text(
                text = "已离线的漫画列表 (${state.offlineComics.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.offlineComics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无离线下载内容",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(state.offlineComics, key = { it.comicId }) { item ->
                        OfflineComicRow(
                            item = item,
                            onDeleteClick = { viewModel.deleteComicDownloads(item.comicId) },
                            enabled = !state.isClearing
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineComicRow(
    item: OfflineComicItem,
    onDeleteClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp, 80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "已离线 ${item.downloadedEpisodesCount} 个章节",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "占用体积 ${formatSize(item.sizeInBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onDeleteClick,
                enabled = enabled,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "删除"
                )
            }
        }
    }
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0.00 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.toDouble())).toInt()
    return DecimalFormat("#,##0.00").format(bytes / Math.pow(1024.toDouble(), digitGroups.toDouble())) + " " + units[digitGroups]
}
