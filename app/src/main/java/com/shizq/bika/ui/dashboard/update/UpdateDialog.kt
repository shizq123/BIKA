package com.shizq.bika.ui.dashboard.update

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

/**
 * 版本更新对话框。
 *
 * 拥有独立的 [UpdateViewModel]，状态和操作全部内聚在 update 包内。
 * 父级只需在合适位置调用 `UpdateDialog()`，无需传入任何参数。
 */
@Composable
fun UpdateDialog(
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Screen 挂载时触发一次版本检测
    LaunchedEffect(Unit) {
//        viewModel.dispatch(UpdateAction.CheckUpdate(BuildConfig.VERSION_NAME))
    }

    // 下载完成后自动触发系统安装器，然后重置状态
    LaunchedEffect(state) {
        if (state is UpdateUiState.Success) {
            runCatching {
                val apkFile = File((state as UpdateUiState.Success).apkPath)
                val apkUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile,
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            viewModel.dispatch(UpdateAction.Reset)
        }
    }

    UpdateDialogContent(
        state = state,
        onDismiss = { viewModel.dispatch(UpdateAction.Reset) },
        onConfirmUpdate = { downloadUrl ->
            viewModel.dispatch(
                UpdateAction.StartDownload(
                    downloadUrl = downloadUrl,
                    externalFilesDir = context.getExternalFilesDir(null)!!,
                )
            )
        },
    )
}

@Composable
fun UpdateDialogContent(
    state: UpdateUiState,
    onDismiss: () -> Unit,
    onConfirmUpdate: (downloadUrl: String) -> Unit,
) {
    when (state) {
        is UpdateUiState.HasUpdate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("发现新版本 v${state.remoteVersion}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("检测到有最新版可以更新，是否立即升级？")
                        if (state.changelog.isNotEmpty()) {
                            Text(
                                text = "更新日志：\n${state.changelog}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { onConfirmUpdate(state.downloadUrl) }) {
                        Text("立即更新")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("稍后")
                    }
                },
            )
        }

        is UpdateUiState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("正在下载更新...") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "已下载: ${(state.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                confirmButton = {},
            )
        }

        is UpdateUiState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("更新失败") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("确定")
                    }
                },
            )
        }

        // Success 由上方 LaunchedEffect 处理安装，Idle 不显示任何内容
        is UpdateUiState.Success,
        UpdateUiState.Idle -> Unit

        else -> {}
    }
}
