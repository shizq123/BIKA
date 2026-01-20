package com.shizq.bika.ui.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
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

class BasicInfoState {
    var nickname by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var passwordVisible by mutableStateOf(false)

    // 追踪字段是否被触碰过
    var nicknameTouched by mutableStateOf(false)
    var usernameTouched by mutableStateOf(false)
    var passwordTouched by mutableStateOf(false)

    fun toData() = BasicInfoData(
        nickname = nickname.trim(),
        username = username.trim(),
        password = password
    )

    fun validate(): ValidationResult {
        nicknameTouched = true
        usernameTouched = true
        passwordTouched = true

        return validateNickname().takeIfError()
            ?: validateUsername().takeIfError()
            ?: validatePassword()
    }

    fun validateNickname(): ValidationResult = when {
        nickname.isBlank() -> ValidationResult.Error("请输入昵称")
        nickname.trim().length < 2 -> ValidationResult.Error("昵称至少需要2个字符")
        nickname.trim().length > 50 -> ValidationResult.Error("昵称不能超过50个字符")
        else -> ValidationResult.Success
    }

    fun validateUsername(): ValidationResult {
        val trimmed = username.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult.Error("请输入用户名")
            !trimmed.matches(USERNAME_REGEX) -> ValidationResult.Error("用户名只能包含字母和数字")
            else -> ValidationResult.Success
        }
    }

    fun validatePassword(): ValidationResult = when {
        password.isEmpty() -> ValidationResult.Error("请输入密码")
        password.length < 8 -> ValidationResult.Error("密码至少需要8个字符")
        else -> ValidationResult.Success
    }

    private fun ValidationResult.takeIfError(): ValidationResult? =
        this as? ValidationResult.Error

    companion object {
        private val USERNAME_REGEX = "^[a-zA-Z0-9]+$".toRegex()
    }
}

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
        SignUpTextField(
            value = state.nickname,
            onValueChange = { state.nickname = it },
            label = "昵称",
            supportingText = "2-50个字符",
            isError = errorMessage != null && state.validateNickname() is ValidationResult.Error,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        SignUpTextField(
            value = state.username,
            onValueChange = { state.username = it },
            label = "用户名",
            supportingText = "用户名不能包含特殊符号",
            isError = errorMessage != null && state.validateUsername() is ValidationResult.Error,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        SignUpTextField(
            value = state.password,
            onValueChange = { state.password = it },
            label = "密码",
            supportingText = "至少8个字符",
            isError = errorMessage != null && state.validatePassword() is ValidationResult.Error,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            visualTransformation = if (state.passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { state.passwordVisible = !state.passwordVisible }) {
                    Icon(
                        imageVector = if (state.passwordVisible) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        },
                        contentDescription = if (state.passwordVisible) "隐藏密码" else "显示密码"
                    )
                }
            }
        )
    }
}

@Composable
private fun SignUpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = { Text(supportingText) },
        isError = isError,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        modifier = modifier.fillMaxWidth()
    )
}