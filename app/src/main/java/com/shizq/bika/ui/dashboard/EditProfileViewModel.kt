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

private val logger = KotlinLogging.logger("EditProfileVM")

/**
 * 修改资料对话框的状态宿主。
 *
 * 作用域是 EditProfileNavKey 这个 entry，不是仪表盘 —— 对话框关闭即随 entry
 * 一起销毁，提交中标志和结果不会泄漏到下一次打开。这也是它没有复用
 * DashboardViewModel 的原因：对话框成了独立 entry 后 `hiltViewModel()` 会拿到
 * 另一个 DashboardViewModel 实例，状态根本不通。
 *
 * 写成功后调 [UserRepository.refreshUserProfile]，仪表盘的资料卡靠共享的
 * userProfile 流收到更新，两边不需要互相知道。
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState = _uiState.asStateFlow()

    fun updateSlogan(slogan: String) {
        // 已有请求在飞时忽略重复提交，避免两次 refresh 打乱结果顺序
        if (_uiState.value.isSubmitting) return

        viewModelScope.launch {
            // runCatching 只包住网络调用：refresh 只是刷新本地 profile 流，
            // 它的失败不代表签名没改成功，混进来会让用户看到「更新失败」
            // 而服务端其实已经生效。
            val result = try {
                _uiState.update { it.copy(isSubmitting = true, result = null) }
                runCatching { userRepository.updateSlogan(slogan) }
            } finally {
                // finally 而非顺序赋值：协程被取消时也要清掉标志，
                // 否则 isSubmitting 停在 true，对话框永久禁用。
                _uiState.update { it.copy(isSubmitting = false) }
            }

            result
                .onSuccess { userRepository.refreshUserProfile() }
                .onFailure { logger.error(it) { "更新自我介绍失败" } }

            _uiState.update {
                it.copy(
                    result = if (result.isSuccess) {
                        OperationResult.Success
                    } else {
                        OperationResult.Error(
                            result.exceptionOrNull()?.localizedMessage ?: "更新失败"
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

data class EditProfileUiState(
    val isSubmitting: Boolean = false,
    val result: OperationResult? = null,
)
