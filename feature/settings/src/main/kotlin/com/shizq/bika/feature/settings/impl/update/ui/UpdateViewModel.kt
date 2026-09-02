package com.shizq.bika.feature.settings.impl.update.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.core.message.MessageReporter
import com.shizq.bika.core.message.UiText
import com.shizq.bika.core.message.reportInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class UpdateViewModel @Inject constructor(
    updateStateMachine: UpdateStateMachine,
    effectEmitter: UpdateEffectEmitter,
    private val messageReporter: MessageReporter,
) : ViewModel() {

    private val stateMachine = updateStateMachine.launchIn(viewModelScope)

    val state = stateMachine.state

    val effects = effectEmitter.effects

    init {
        // 手动检查（用户主动点击"检查更新"）时，NoUpdate 需要明确反馈；
        // Auto 静默检查则不打断用户，交由统一的 Snackbar 消息通道展示。
        state.onEach { currentState ->
            if (currentState is UpdateUiState.NoUpdate && currentState.source == UpdateCheckSource.Manual) {
                messageReporter.reportInfo(UiText.of("当前已是最新版本"))
                dispatch(UpdateAction.Reset)
            }
        }.launchIn(viewModelScope)
    }

    fun dispatch(action: UpdateAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }
}