package com.shizq.bika.ui.signin

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.core.ui.CircularProgressIndicator

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToDashboard: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
) {
    val loginState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(loginState.isSuccess) {
        if (loginState.isSuccess) {
            Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
            onNavigateToDashboard()
            viewModel.dispatch(LoginAction.ClearError)
        }
    }

    LaunchedEffect(loginState.errorMessage) {
        if (!loginState.errorMessage.isNullOrBlank()) {
            Toast.makeText(context, loginState.errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.dispatch(LoginAction.ClearError)
        }
    }

    LoginContent(
        loginState = loginState,
        onRememberPasswordChange = { viewModel.dispatch(LoginAction.ToggleRememberPassword(it)) },
        onNavigateToSignUp = onNavigateToSignUp,
        onNavigateToForgotPassword = {
            onNavigateToForgotPassword()
            Toast.makeText(context, "功能暂不可用", Toast.LENGTH_SHORT).show()
        },
        dispatch = viewModel::dispatch,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginContent(
    loginState: LoginUiState,
    modifier: Modifier = Modifier,
    onRememberPasswordChange: (Boolean) -> Unit = {},
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    dispatch: (LoginAction) -> Unit,
) {
    val passwordFocusRequester = remember { FocusRequester() }

    val loginButtonEnabled = loginState.username.isNotBlank()
            && loginState.password.isNotBlank()
            && !loginState.isAuthenticating

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "BIKA",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "请登录您的账号",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(48.dp))

                // 账号输入框
                EmailTextField(
                    email = loginState.username,
                    onEmailChange = { dispatch(LoginAction.AccountChanged(it)) },
                    onNext = { passwordFocusRequester.requestFocus() },
                    isError = loginState.usernameIsEmpty,
                    modifier = Modifier
                        .semantics {
                            contentType = ContentType.Username
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 密码输入框
                PasswordTextField(
                    password = loginState.password,
                    onPasswordChange = { dispatch(LoginAction.PasswordChanged(it)) },
                    isError = loginState.passwordIsEmpty,
                    modifier = Modifier
                        .semantics {
                            contentType = ContentType.Password
                        }
                        .focusRequester(passwordFocusRequester),
                    onDone = {
                        if (loginButtonEnabled) {
                            dispatch(LoginAction.SubmitLogin)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = loginState.rememberPassword,
                        onCheckedChange = onRememberPasswordChange
                    )
                    Text(
                        text = "记住密码",
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = onNavigateToForgotPassword,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "忘记密码？",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        dispatch(LoginAction.SubmitLogin)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    enabled = loginButtonEnabled
                ) {
                    if (loginState.isAuthenticating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "登录",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 注册提示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "还没有账号？",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = onNavigateToSignUp,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "立即注册",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(128.dp))
            }
        }
    }
}

@Composable
fun EmailTextField(
    email: String,
    onEmailChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onNext: () -> Unit = {}
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        modifier = modifier.fillMaxWidth(),
        label = {
            Text(
                text = "用户名或邮箱",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() }
        ),
        singleLine = true,
        isError = isError,
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        supportingText = {
            if (isError) {
                Text(
                    "用户名不能为空",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        trailingIcon = {
            if (email.isNotEmpty()) {
                IconButton(
                    onClick = { onEmailChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清空",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Composable
fun PasswordTextField(
    password: String,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onDone: () -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        modifier = modifier
            .fillMaxWidth(),
        label = {
            Text(
                text = "密码",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        singleLine = true,
        isError = isError,
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDone() }
        ),
        supportingText = {
            if (isError) {
                Text(
                    "密码不能为空",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        trailingIcon = {
            Row {
                val image = if (passwordVisible) {
                    Icons.Filled.Visibility
                } else {
                    Icons.Filled.VisibilityOff
                }
                val description = if (passwordVisible) "隐藏密码" else "显示密码"

                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = image,
                        contentDescription = description,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Preview
@Composable
private fun LoginScreenPreview() {
    LoginScreen(
        onNavigateToDashboard = { },
        onNavigateToSignUp = { },
        onNavigateToForgotPassword = {}
    )
}