package com.shizq.bika.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.core.ui.CircularProgressIndicator

/**
 * 修改资料对话框的 entry 入口。状态归 [EditProfileViewModel]，作用域是本 entry。
 *
 * [initialSlogan] 从 EditProfileNavKey 传入，见那里的注释。
 */
@Composable
fun EditProfileDialog(
    initialSlogan: String,
    onDismiss: () -> Unit,
    onChangePasswordClick: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EditProfileDialogContent(
        initialSlogan = initialSlogan,
        sloganResult = uiState.result,
        isSubmitting = uiState.isSubmitting,
        onSave = viewModel::updateSlogan,
        onDismissResult = viewModel::dismissResult,
        onDismiss = onDismiss,
        onChangePasswordClick = onChangePasswordClick,
    )
}

/**
 * 只认参数和回调，不持有任何 ViewModel，便于预览和测试。
 *
 * [initialSlogan] 作为 remember 的初值，避免靠 LaunchedEffect 异步赋值导致的一帧闪烁。
 */
@Composable
fun EditProfileDialogContent(
    initialSlogan: String,
    sloganResult: OperationResult?,
    isSubmitting: Boolean,
    onSave: (String) -> Unit,
    onDismissResult: () -> Unit,
    onDismiss: () -> Unit,
    onChangePasswordClick: () -> Unit,
) {
    var inputSlogan by remember { mutableStateOf(initialSlogan) }

    // sloganResult 驱动：成功时关闭对话框，失败时保持打开并显示错误
    LaunchedEffect(sloganResult) {
        if (sloganResult == OperationResult.Success) {
            onDismiss()
            onDismissResult()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("修改资料") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = inputSlogan,
                    onValueChange = { inputSlogan = it },
                    label = { Text("自我介绍") },
                    placeholder = { Text("输入您的个性签名") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )

                if (sloganResult is OperationResult.Error) {
                    Text(
                        text = sloganResult.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isSubmitting) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                TextButton(
                    onClick = onChangePasswordClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("修改密码")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = {
                    onDismissResult()
                    onSave(inputSlogan)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = {
                    onDismiss()
                    onDismissResult()
                }
            ) {
                Text("取消")
            }
        }
    )
}
