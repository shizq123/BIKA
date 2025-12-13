package com.shizq.bika.ui.reader.gesture

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun Modifier.volumeKeyHandler(
    enabled: Boolean,
    scope: CoroutineScope,
    onVolumeUp: suspend () -> Unit,
    onVolumeDown: suspend () -> Unit
): Modifier = this.onPreviewKeyEvent { event ->
    if (!enabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

    when (event.key) {
        Key.VolumeDown -> {
            scope.launch { onVolumeDown() }
            true
        }

        Key.VolumeUp -> {
            scope.launch { onVolumeUp() }
            true
        }

        else -> false
    }
}