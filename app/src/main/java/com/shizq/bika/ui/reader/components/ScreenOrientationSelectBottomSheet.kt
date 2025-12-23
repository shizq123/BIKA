package com.shizq.bika.ui.reader.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.shizq.bika.core.model.ScreenOrientation
@Composable
fun ScreenOrientationSelectBottomSheet(
    orientation: ScreenOrientation,
    onDismissRequest: () -> Unit,
    onOrientationChange: (ScreenOrientation) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentOrientation by remember(orientation) { mutableStateOf(orientation) }

    ModeSelectionBottomSheet(
        title = "屏幕方向",
        onDismissRequest = onDismissRequest,
        onApply = {
            onOrientationChange(currentOrientation)
            onDismissRequest()
        },
        isApplyEnabled = currentOrientation != orientation,
        modifier = modifier,
    ) {
        OptionFlowRow(
            options = ScreenOrientation.entries,
            selectedOption = currentOrientation,
            onOptionSelected = { newMode -> currentOrientation = newMode },
            labelProvider = { it.label }
        )
    }
}