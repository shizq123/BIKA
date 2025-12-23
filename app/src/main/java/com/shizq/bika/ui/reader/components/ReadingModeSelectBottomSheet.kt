package com.shizq.bika.ui.reader.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.shizq.bika.core.model.ReadingMode

@Composable
fun ReadingModeSelectBottomSheet(
    activeMode: ReadingMode,
    onDismissRequest: () -> Unit,
    onReadingModeChanged: (ReadingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember(activeMode) { mutableStateOf(activeMode) }

    ModeSelectionBottomSheet(
        title = "阅读模式",
        onDismissRequest = onDismissRequest,
        onApply = {
            onReadingModeChanged(selectedMode)
            onDismissRequest()
        },
        isApplyEnabled = selectedMode != activeMode,
        modifier = modifier,
    ) {
        OptionFlowRow(
            options = ReadingMode.entries,
            selectedOption = selectedMode,
            onOptionSelected = { newMode -> selectedMode = newMode },
            labelProvider = { it.label }
        )
    }
}