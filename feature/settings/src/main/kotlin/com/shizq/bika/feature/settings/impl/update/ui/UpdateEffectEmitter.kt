package com.shizq.bika.feature.settings.impl.update.ui

import jakarta.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class UpdateEffectEmitter @Inject constructor() {

    // 下载结束时界面可能不在前台；缓存事件，恢复后消费一次。
    private val pendingEffects = Channel<UpdateUiEffect>(Channel.BUFFERED)

    val effects = pendingEffects.receiveAsFlow()

    suspend fun emit(effect: UpdateUiEffect) {
        pendingEffects.send(effect)
    }
}
