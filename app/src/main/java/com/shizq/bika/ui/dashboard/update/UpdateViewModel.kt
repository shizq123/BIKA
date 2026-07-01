package com.shizq.bika.ui.dashboard.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class UpdateViewModel @Inject constructor(
    updateStateMachine: UpdateStateMachine,
) : ViewModel() {

    private val stateMachine = updateStateMachine.launchIn(viewModelScope)

    val state = stateMachine.state

    fun dispatch(action: UpdateAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }
}