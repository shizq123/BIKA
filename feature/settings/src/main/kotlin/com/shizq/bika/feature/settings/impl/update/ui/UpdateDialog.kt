package com.shizq.bika.feature.settings.impl.update.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.network.model.AppUpdateRelease
import com.shizq.bika.core.ui.CircularProgressIndicator

@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onDismiss: () -> Unit,
    onStartDownload: (AppUpdateRelease) -> Unit,
    onIgnoreVersion: (AppUpdateRelease) -> Unit,
    onRetry: (UpdateRetryAction) -> Unit,
) {
    when (state) {
        is UpdateUiState.HasUpdate -> {
            AlertDialog(
                onDismissRequest = {
                    if (!state.release.forceUpdate) {
                        onDismiss()
                    }
                },
                title = {
                    Text("发现新版本 v${state.release.versionName}")
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            if (state.release.forceUpdate) {
                                "当前版本需要升级后才能继续使用。"
                            } else {
                                "检测到有最新版可以更新，是否立即升级？"
                            },
                        )

                        if (state.release.changelog.isNotBlank()) {
                            Text(
                                text = "更新日志：\n${state.release.changelog}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onStartDownload(state.release)
                        },
                    ) {
                        Text("立即更新")
                    }
                },
                dismissButton = {
                    if (!state.release.forceUpdate) {
                        Column {
                            TextButton(
                                onClick = onDismiss,
                            ) {
                                Text("稍后")
                            }

                            TextButton(
                                onClick = {
                                    onIgnoreVersion(state.release)
                                },
                            ) {
                                Text("忽略此版本")
                            }
                        }
                    }
                },
            )
        }

        is UpdateUiState.Downloading -> {
            AlertDialog(
                onDismissRequest = {
                    // 当前暂不支持取消下载，禁止外部关闭
                },
                title = {
                    Text("正在下载更新...")
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        LinearProgressIndicator(
                            progress = {
                                state.progress
                            },
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
                title = {
                    Text("更新失败")
                },
                text = {
                    Text(state.message)
                },
                confirmButton = {
                    if (state.canRetry && state.retryAction != null) {
                        TextButton(
                            onClick = {
                                onRetry(state.retryAction)
                            },
                        ) {
                            Text("重试")
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                        ) {
                            Text("确定")
                        }
                    }
                },
                dismissButton = {
                    if (state.canRetry) {
                        TextButton(
                            onClick = onDismiss,
                        ) {
                            Text("取消")
                        }
                    }
                },
            )
        }

        is UpdateUiState.Checking -> {
            // Auto 来源静默检查，不打断用户；Manual（用户主动点击"检查更新"）展示进度反馈
            if (state.source == UpdateCheckSource.Manual) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    title = { Text("正在检查更新") },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CircularProgressIndicator()
                            Text("请稍候...")
                        }
                    },
                )
            }
        }

        UpdateUiState.Idle,
        is UpdateUiState.NoUpdate,
        UpdateUiState.Ignored -> {
            // 不展示：NoUpdate/Ignored 的用户反馈（如手动检查后的 Toast）由 UpdateHost 统一处理
        }
    }
}