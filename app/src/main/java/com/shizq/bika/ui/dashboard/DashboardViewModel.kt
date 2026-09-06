package com.shizq.bika.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    stateMachineFactory: DashboardStateMachine,
) : ViewModel() {

    private val stateMachine = stateMachineFactory.launchIn(viewModelScope)

    val state = stateMachine.state

    fun dispatch(action: DashboardAction) {
        viewModelScope.launch {
            stateMachine.dispatch(action)
        }
    }
}
