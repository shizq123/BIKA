package com.shizq.bika.feature.update.ui

import dagger.hilt.android.scopes.ViewModelScoped
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@ViewModelScoped
class UpdateEffectEmitter @Inject constructor() {

    private val _effects = MutableSharedFlow<UpdateUiEffect>(
        extraBufferCapacity = 1,
    )

    val effects = _effects.asSharedFlow()

    suspend fun emit(effect: UpdateUiEffect) {
        _effects.emit(effect)
    }

    fun tryEmit(effect: UpdateUiEffect) {
        _effects.tryEmit(effect)
    }
}