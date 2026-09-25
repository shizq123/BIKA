package com.shizq.bika.feature.comicdetail.impl.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.datastore.UserPreferencesDataSource
import com.shizq.bika.core.message.MessageReporter
import com.shizq.bika.core.message.UiText
import com.shizq.bika.core.message.reportInfo
import com.shizq.bika.feature.comicdetail.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 标签屏蔽对话框的业务状态。生命周期由对话框导航 entry 持有。 */
@HiltViewModel
class TagBlockViewModel @Inject constructor(
    private val preferences: UserPreferencesDataSource,
    private val messageReporter: MessageReporter,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TagBlockUiState())
    val uiState = _uiState.asStateFlow()

    fun confirm(tag: String) {
        if (_uiState.value.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                preferences.addBlockedTag(tag)
                messageReporter.reportInfo(UiText.of(R.string.tag_blocked, tag))
                _uiState.update { it.copy(isSubmitting = false, completed = true) }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        error = throwable.localizedMessage ?: "屏蔽标签失败",
                    )
                }
            }
        }
    }

    fun clearCompleted() {
        _uiState.update { it.copy(completed = false) }
    }
}

data class TagBlockUiState(
    val isSubmitting: Boolean = false,
    val completed: Boolean = false,
    val error: String? = null,
)
