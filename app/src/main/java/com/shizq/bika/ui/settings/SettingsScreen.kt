package com.shizq.bika.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import com.shizq.bika.core.ui.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.core.model.DarkThemeConfig
import com.shizq.bika.core.model.NetworkLine

@Composable
fun SettingsScreen(
    navigationToLogin: () -> Unit,
    navigationToStorageManager: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
) {
    val settingsUiState by viewModel.settingsUiState.collectAsStateWithLifecycle()
    val cacheSize by viewModel.cacheSize.collectAsStateWithLifecycle()
    val updateUiState by viewModel.updateUiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(updateUiState) {
        if (updateUiState is UpdateUiState.NoUpdate) {
            android.widget.Toast.makeText(context, "当前已是最新版本", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetUpdateState()
        } else if (updateUiState is UpdateUiState.Error) {
            val errorState = updateUiState as UpdateUiState.Error
            android.widget.Toast.makeText(context, "检查更新失败: ${errorState.message}", android.widget.Toast.LENGTH_LONG).show()
            viewModel.resetUpdateState()
        }
    }

    if (updateUiState is UpdateUiState.Checking) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("正在检查更新") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("请稍候...")
                }
            }
        )
    }

    val hasUpdateState = updateUiState as? UpdateUiState.HasUpdate
    if (hasUpdateState != null) {
        AlertDialog(
            onDismissRequest = { viewModel.resetUpdateState() },
            title = { Text("发现新版本 (${hasUpdateState.version})") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = hasUpdateState.body.ifBlank { "无更新说明" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        uriHandler.openUri(hasUpdateState.url)
                        viewModel.resetUpdateState()
                    }
                ) {
                    Text("去下载")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetUpdateState() }) {
                    Text("取消")
                }
            }
        )
    }

    var showLogsDialog by remember { mutableStateOf(false) }
    var logsContent by remember { mutableStateOf("") }

    if (showLogsDialog) {
        LogViewerDialog(
            logs = logsContent,
            onCopy = {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("logs", logsContent)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(context, "已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
            },
            onClear = {
                viewModel.clearLogs()
                logsContent = ""
                android.widget.Toast.makeText(context, "日志已清空", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showLogsDialog = false }
        )
    }

    SettingsContent(
        settingsUiState = settingsUiState,
        cacheSize = cacheSize,
        onClearCache = viewModel::clearCache,
        onUpdateDarkThemeConfig = viewModel::updateDarkThemeConfig,
        onToggleAutoCheckIn = viewModel::updateAutoCheckIn,
        onUpdateNetworkLine = viewModel::updateSelectedNetworkLine,
        onUpdateFontScale = viewModel::updateFontScale,
        onToggleIsLoggingEnabled = viewModel::updateIsLoggingEnabled,
        onToggleDownloadOverWifiOnly = viewModel::updateDownloadOverWifiOnly,
        onUpdateMaxConcurrentDownloads = viewModel::updateMaxConcurrentDownloads,
        onViewLogs = {
            logsContent = viewModel.getLogsContent()
            showLogsDialog = true
        },
        onExportLogs = {
            val logFile = com.shizq.bika.core.common.BikaLog.getLogFile()
            if (logFile != null && logFile.exists() && logFile.length() > 0) {
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        logFile
                    )
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "text/plain"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "导出日志"))
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "导出失败: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                android.widget.Toast.makeText(context, "日志为空，请先开启日志开关并操作产生日志后再来查看", android.widget.Toast.LENGTH_SHORT).show()
            }
        },
        onLogoutClicked = {
            viewModel.logout()
            navigationToLogin()
        },
        onStorageManagerClick = navigationToStorageManager,
        onBackClick = onBackClick,
        onCheckForUpdates = viewModel::checkForUpdates,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    settingsUiState: SettingsUiState,
    cacheSize: String,
    onClearCache: () -> Unit = {},
    onStorageManagerClick: () -> Unit = {},
    onUpdateDarkThemeConfig: (config: DarkThemeConfig) -> Unit = {},
    onToggleAutoCheckIn: (enabled: Boolean) -> Unit = {},
    onUpdateNetworkLine: (line: NetworkLine) -> Unit = {},
    onUpdateFontScale: (scale: Float) -> Unit = {},
    onToggleIsLoggingEnabled: (enabled: Boolean) -> Unit = {},
    onToggleDownloadOverWifiOnly: (enabled: Boolean) -> Unit = {},
    onUpdateMaxConcurrentDownloads: (count: Int) -> Unit = {},
    onViewLogs: () -> Unit = {},
    onExportLogs: () -> Unit = {},
    onLogoutClicked: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                }
            )
        }
    ) { paddingValues ->
        when (settingsUiState) {
            SettingsUiState.Loading -> {}
            is SettingsUiState.Success ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    item {
                        PreferenceGroup(title = { Text("常规") }) {
                            var showDialog by remember { mutableStateOf(false) }

                            if (showDialog) {
                                AlertDialog(
                                    onDismissRequest = { showDialog = false },
                                    title = { Text(text = "确认清理") },
                                    text = { Text(text = "确定要清理所有图片缓存吗？此操作不可撤销。") },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                onClearCache()
                                                showDialog = false
                                            }
                                        ) {
                                            Text("确定")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDialog = false }) {
                                            Text("取消")
                                        }
                                    }
                                )
                            }
                            Preference(
                                title = "清理图片缓存",
                                summary = cacheSize,
                                iconVector = Icons.Default.Close,
                                onClick = { showDialog = true }
                            )
                            Preference(
                                title = "存储与空间管理",
                                summary = "管理图片缓存及已离线的章节",
                                iconVector = Icons.AutoMirrored.Filled.List,
                                onClick = onStorageManagerClick
                            )
                            ListPreference(
                                title = "选择网络分流",
                                iconVector = Icons.AutoMirrored.Filled.List,
                                options = NetworkLine.entries,
                                selectedValue = settingsUiState.selectedNetworkLine,
                                optionToText = { it.display },
                                onOptionSelected = onUpdateNetworkLine
                            )
                            SwitchPreference(
                                title = "自动签到",
                                summary = if (settingsUiState.autoCheckIn) "开启" else "关闭",
                                iconVector = Icons.Default.ThumbUp,
                                checked = settingsUiState.autoCheckIn,
                                onCheckedChange = onToggleAutoCheckIn
                            )
                            ListPreference(
                                title = "夜间模式",
                                iconVector = Icons.Default.DarkMode,
                                options = DarkThemeConfig.entries,
                                selectedValue = settingsUiState.darkThemeConfig,
                                optionToText = { it.title },
                                onOptionSelected = onUpdateDarkThemeConfig
                            )
                            ListPreference(
                                title = "文字大小",
                                iconVector = Icons.Default.FormatSize,
                                options = listOf(0.85f, 1.0f, 1.15f, 1.3f),
                                selectedValue = settingsUiState.fontScale,
                                optionToText = {
                                    when (it) {
                                        0.85f -> "小"
                                        1.0f -> "标准"
                                        1.15f -> "大"
                                        1.3f -> "特大"
                                        else -> "标准"
                                    }
                                },
                                onOptionSelected = onUpdateFontScale
                            )
                        }
                    }

                    item {
                        PreferenceGroup(title = { Text("账号") }) {
                            Preference(
                                title = "退出登录",
                                summary = "退出当前账号",
                                iconVector = Icons.AutoMirrored.Filled.ExitToApp,
                                onClick = {
                                    onLogoutClicked()
                                }
                            )
                        }
                    }

                    item {
                        PreferenceGroup(title = { Text("官方") }) {
                            Preference(
                                title = "哔咔网页版",
                                onClick = { uriHandler.openUri("https://manhuabika.com/") }
                            )
                            Preference(
                                title = "Wiki",
                                onClick = { uriHandler.openUri("http://picawiki.xyz/") }
                            )
                        }
                    }

                    item {
                        PreferenceGroup(title = { Text("下载配置") }) {
                            SwitchPreference(
                                title = "仅 Wi-Fi 下载",
                                summary = if (settingsUiState.downloadOverWifiOnly) "仅在连接 Wi-Fi 时执行下载任务" else "允许在移动数据下下载（可能产生流量费用）",
                                iconVector = Icons.Default.Wifi,
                                checked = settingsUiState.downloadOverWifiOnly,
                                onCheckedChange = onToggleDownloadOverWifiOnly
                            )
                            ListPreference(
                                title = "最大并发下载数",
                                iconVector = Icons.AutoMirrored.Filled.List,
                                options = listOf(1, 2, 3, 5),
                                selectedValue = settingsUiState.maxConcurrentDownloads,
                                optionToText = { "$it 个章节" },
                                onOptionSelected = onUpdateMaxConcurrentDownloads
                            )
                        }
                    }

                    item {
                        PreferenceGroup(title = { Text("调试与日志") }) {
                            SwitchPreference(
                                title = "调试日志开关",
                                summary = if (settingsUiState.isLoggingEnabled) "已开启本地日志追加" else "本地日志已关闭",
                                iconVector = Icons.Default.Code,
                                checked = settingsUiState.isLoggingEnabled,
                                onCheckedChange = onToggleIsLoggingEnabled
                            )
                            Preference(
                                title = "查看系统日志",
                                summary = "查看本地已收集的系统运行日志",
                                iconVector = Icons.Default.Description,
                                onClick = onViewLogs
                            )
                            Preference(
                                title = "导出系统日志",
                                summary = "分享或导出本地系统日志文件",
                                iconVector = Icons.Default.Share,
                                onClick = onExportLogs
                            )
                        }
                    }

                    item {
                        PreferenceGroup(title = { Text("应用") }) {
                            Preference(
                                title = "检查更新",
                                summary = "当前版本: ${com.shizq.bika.BuildConfig.VERSION_NAME}",
                                iconVector = Icons.Default.Refresh,
                                onClick = onCheckForUpdates
                            )

                            Preference(
                                title = "应用信息",
                                summary = "在系统设置上查看应用信息",
                                iconVector = Icons.Default.Info,
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        "package:com.shizq.bika".toUri()
                                    )
                                    context.startActivity(intent)
                                }
                            )

                            Preference(
                                title = "GitHub",
                                summary = "在GitHub上查看",
                                iconVector = Icons.Default.Code,
                                onClick = { uriHandler.openUri("https://github.com/shizq123/BIKA") }
                            )
                        }
                    }
                }
        }
    }
}

@Composable
fun LogViewerDialog(
    logs: String,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "系统日志")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onCopy) {
                        Text("复制")
                    }
                    TextButton(onClick = onClear) {
                        Text("清空")
                    }
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = logs.ifBlank { "暂无本地日志，请开启日志开关并操作产生日志后再来查看。" },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}