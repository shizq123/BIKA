package com.shizq.bika.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.core.model.DarkThemeConfig
import com.shizq.bika.core.model.NetworkLine


@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onLogoutClicked: () -> Unit,
) {
    val settingsUiState by viewModel.settingsUiState.collectAsStateWithLifecycle()

    val cacheSize by viewModel.cacheSize.collectAsStateWithLifecycle()
    SettingsContent(
        settingsUiState = settingsUiState,
        cacheSize = cacheSize,
        onClearCache = viewModel::clearCache,
        onUpdateDarkThemeConfig = viewModel::updateDarkThemeConfig,
        onToggleAutoCheckIn = viewModel::updateAutoCheckIn,
        onUpdateNetworkLine = viewModel::updateSelectedNetworkLine,
        onLogoutClicked = {
            viewModel.logout()
            onLogoutClicked()
        },
        onBackClick = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    settingsUiState: SettingsUiState,
    cacheSize: String,
    onClearCache: () -> Unit = {},
    onUpdateDarkThemeConfig: (config: DarkThemeConfig) -> Unit = {},
    onToggleAutoCheckIn: (enabled: Boolean) -> Unit = {},
    onUpdateNetworkLine: (line: NetworkLine) -> Unit = {},
    onLogoutClicked: () -> Unit = {},
    onBackClick: () -> Unit = {},
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
//                                        val intent = Intent(
//                                            this@SettingsActivity,
//                                            AccountActivity::class.java
//                                        ).apply {
//                                            flags =
//                                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//                                        }
//                                        startActivity(intent)

//                                        finish()
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
                        PreferenceGroup(title = { Text("应用") }) {
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