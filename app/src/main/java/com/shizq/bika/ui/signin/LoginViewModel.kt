package com.shizq.bika.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    loginStateMachine: LoginStateMachine,
) : ViewModel() {
    private val stateMache = loginStateMachine.launchIn(viewModelScope)
    val state = stateMache.state

    fun dispatch(action: LoginAction) {
        viewModelScope.launch {
            stateMache.dispatch(action)
        }
    }
}