package com.shizq.bika.ui.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.ui.wizard.ValidationResult

data class BasicInfoData(
    val nickname: String = "",
    val username: String = "",
    val password: String = ""
)

// 基本信息状态管理
class BasicInfoState {
    var nickname by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var passwordVisible by mutableStateOf(false)

    fun toData() = BasicInfoData(
        nickname = nickname,
        username = username,
        password = password
    )

    fun validate(): ValidationResult {
        validateNickname().let { result ->
            if (result is ValidationResult.Error) return result
        }
        validateUsername().let { result ->
            if (result is ValidationResult.Error) return result
        }
        return validatePassword()
    }

    fun validateNickname(): ValidationResult {
        return when {
            nickname.isEmpty() -> ValidationResult.Error("请输入昵称")
            nickname.length < 2 -> ValidationResult.Error("昵称必须至少2个字符")
            nickname.length > 50 -> ValidationResult.Error("昵称不能超过50个字符")
            else -> ValidationResult.Success
        }
    }

    fun validateUsername(): ValidationResult {
        val usernameRegex = "^[a-zA-Z0-9]+$".toRegex()
        return when {
            username.isEmpty() -> ValidationResult.Error("请输入用户名")
            !username.matches(usernameRegex) -> ValidationResult.Error("用户名只能包含字母和数字，不能包含特殊符号")
            else -> ValidationResult.Success
        }
    }

    fun validatePassword(): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult.Error("请输入密码")
            password.length <= 8 -> ValidationResult.Error("密码必须大于8个字符")
            else -> ValidationResult.Success
        }
    }
}

// 基本信息输入组件
@Composable
fun BasicInfoStep(
    state: BasicInfoState,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 昵称输入
        NicknameField(
            value = state.nickname,
            onValueChange = { state.nickname = it },
            isError = errorMessage != null &&
                    state.validateNickname() is ValidationResult.Error
        )

        // 用户名输入
        UsernameField(
            value = state.username,
            onValueChange = { state.username = it },
            isError = errorMessage != null &&
                    state.validateUsername() is ValidationResult.Error,
            modifier = Modifier
        )

        // 密码输入
        PasswordField(
            value = state.password,
            onValueChange = { state.password = it },
            passwordVisible = state.passwordVisible,
            onPasswordVisibilityChange = { state.passwordVisible = it },
            isError = errorMessage != null &&
                    state.validatePassword() is ValidationResult.Error
        )
    }
}

// 昵称输入字段
@Composable
fun NicknameField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("昵称") },
        supportingText = {
            Text("昵称必须在2到50个字符之间")
        },
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        singleLine = true
    )
}

// 用户名输入字段
@Composable
fun UsernameField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("用户名") },
        supportingText = {
            Text("只能包含字母和数字")
        },
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default,
        keyboardActions = KeyboardActions(),
    )
}

// 密码输入字段
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("密码") },
        supportingText = {
            Text("密码必须大于8个字符")
        },
        isError = isError,
        visualTransformation = if (passwordVisible)
            VisualTransformation.None
        else
            PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                Icon(
                    imageVector = if (passwordVisible)
                        Icons.Default.Visibility
                    else
                        Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible)
                        "隐藏密码"
                    else
                        "显示密码"
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}

// 信息确认组件
@Composable
fun BasicInfoConfirmation(
    data: BasicInfoData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "请确认您的信息",
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider()
            InfoRow(label = "昵称", value = data.nickname)
            InfoRow(label = "用户名", value = data.username)
            InfoRow(label = "密码", value = "••••••••")
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}