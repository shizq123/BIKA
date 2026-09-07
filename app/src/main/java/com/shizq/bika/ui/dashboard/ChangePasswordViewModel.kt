package com.shizq.bika.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger("ChangePasswordVM")

/**
 * 修改密码对话框的状态宿主。作用域是 ChangePasswordNavKey 这个 entry。
 *
 * 不调 refreshUserProfile：改密码不影响资料卡内容。
 */
@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun changePassword(oldPassword: String, newPassword: String) {
        if (_uiState.value.isSubmitting) return

        viewModelScope.launch {
            val result = try {
                _uiState.update { it.copy(isSubmitting = true, result = null) }
                runCatching { userRepository.changePassword(oldPassword, newPassword) }
            } finally {
                // 见 EditProfileViewModel：取消时也要清标志
                _uiState.update { it.copy(isSubmitting = false) }
            }

            result.onFailure { logger.error(it) { "修改密码失败" } }

            _uiState.update {
                it.copy(
                    result = if (result.isSuccess) {
                        OperationResult.Success
                    } else {
                        OperationResult.Error(
                            result.exceptionOrNull()?.localizedMessage ?: "修改失败"
                        )
                    }
                )
            }
        }
    }

    fun dismissResult() {
        _uiState.update { it.copy(result = null) }
    }
}

data class ChangePasswordUiState(
    val isSubmitting: Boolean = false,
    val result: OperationResult? = null,
)
