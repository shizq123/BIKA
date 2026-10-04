package com.shizq.bika.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shizq.bika.navigation.DiscoveryAction
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

/** Android 生命周期适配层；Feed 的状态转移与副作用由 [FeedStateMachine] 负责。 */
@HiltViewModel(assistedFactory = FeedViewModel.Factory::class)
class FeedViewModel @AssistedInject constructor(
    stateMachineFactory: FeedStateMachineFactory,
    @Assisted val currentAction: DiscoveryAction,
) : ViewModel() {

    private val stateMachine = stateMachineFactory
        .create(currentAction)
        .launchIn(viewModelScope)

    val uiState = stateMachine.state

    fun dispatch(action: FeedAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(action: DiscoveryAction): FeedViewModel
    }
}
