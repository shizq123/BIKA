package com.shizq.bika.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.core.ui.CircularProgressIndicator

/** 修改密码对话框的 entry 入口。状态归 [ChangePasswordViewModel]，作用域是本 entry。 */
@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChangePasswordDialogContent(
        passwordResult = uiState.result,
        isSubmitting = uiState.isSubmitting,
        onSave = viewModel::changePassword,
        onDismissResult = viewModel::dismissResult,
        onDismiss = onDismiss,
    )
}

/** 只认参数和回调，不持有任何 ViewModel，便于预览和测试。 */
@Composable
fun ChangePasswordDialogContent(
    passwordResult: OperationResult?,
    isSubmitting: Boolean,
    onSave: (oldPassword: String, newPassword: String) -> Unit,
    onDismissResult: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    var inputOldPassword by remember { mutableStateOf("") }
    var inputNewPassword by remember { mutableStateOf("") }
    var inputConfirmPassword by remember { mutableStateOf("") }
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    // 本地校验错误（未发到服务端前）
    var localPasswordError by remember { mutableStateOf<String?>(null) }

    // 打开时清掉上一轮遗留的服务端结果，避免刚进来就显示旧错误
    LaunchedEffect(Unit) {
        onDismissResult()
    }

    // passwordResult 驱动：成功时 Toast + 关闭，失败时保持打开
    LaunchedEffect(passwordResult) {
        if (passwordResult == OperationResult.Success) {
            Toast.makeText(context, "密码修改成功", Toast.LENGTH_SHORT).show()
            onDismiss()
            onDismissResult()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("修改密码") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = inputOldPassword,
                    onValueChange = { inputOldPassword = it },
                    label = { Text("旧密码") },
                    placeholder = { Text("请输入旧密码") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                            Icon(
                                imageVector = if (oldPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (oldPasswordVisible) "隐藏旧密码" else "显示旧密码"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = inputNewPassword,
                    onValueChange = { inputNewPassword = it },
                    label = { Text("新密码") },
                    placeholder = { Text("请输入新密码（至少8位）") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                imageVector = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (newPasswordVisible) "隐藏新密码" else "显示新密码"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = inputConfirmPassword,
                    onValueChange = { inputConfirmPassword = it },
                    label = { Text("确认新密码") },
                    placeholder = { Text("请再次输入新密码") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = {
                            confirmPasswordVisible = !confirmPasswordVisible
                        }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "隐藏确认密码" else "显示确认密码"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // 本地校验错误优先，服务端错误次之
                val displayError = localPasswordError
                    ?: (passwordResult as? OperationResult.Error)?.message
                if (displayError != null) {
                    Text(
                        text = displayError,
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
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = {
                    localPasswordError = when {
                        inputOldPassword.isEmpty() -> "请输入旧密码"
                        inputNewPassword.isEmpty() -> "请输入新密码"
                        inputNewPassword.length < 8 -> "新密码长度至少需要8个字符"
                        inputNewPassword != inputConfirmPassword -> "两次输入的新密码不一致"
                        else -> null
                    }
                    if (localPasswordError != null) return@TextButton
                    onDismissResult()
                    onSave(inputOldPassword, inputNewPassword)
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
