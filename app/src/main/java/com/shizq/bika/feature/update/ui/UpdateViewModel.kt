package com.shizq.bika.feature.update.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class UpdateViewModel @Inject constructor(
    updateStateMachine: UpdateStateMachine,
    effectEmitter: UpdateEffectEmitter,
) : ViewModel() {

    private val stateMachine = updateStateMachine.launchIn(viewModelScope)

    val state = stateMachine.state

    val effects = effectEmitter.effects

    fun dispatch(action: UpdateAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }
}