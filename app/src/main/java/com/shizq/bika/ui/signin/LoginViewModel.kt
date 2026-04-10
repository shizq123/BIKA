package com.shizq.bika.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    loginStateMachine: LoginStateMachineFactory,
) : ViewModel() {
    private val stateMachine = loginStateMachine.launchIn(viewModelScope)
    val state = stateMachine.state

    fun dispatch(action: LoginAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }
}