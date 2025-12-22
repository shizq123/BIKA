package com.shizq.bika.ui.reader.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.shizq.bika.R
import com.shizq.bika.core.model.ReadingMode
import com.shizq.bika.ui.reader.settings.OptionFlowRow

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

@DrawableRes
fun ReadingMode.toIconRes(): Int {
    return when (this) {
        ReadingMode.LEFT_TO_RIGHT -> R.drawable.ic_reader_ltr
        ReadingMode.RIGHT_TO_LEFT -> R.drawable.ic_reader_rtl
        ReadingMode.VERTICAL_PAGER -> R.drawable.ic_reader_ttb
        ReadingMode.WEBTOON -> R.drawable.ic_reader_webtoon
        ReadingMode.CONTINUOUS_VERTICAL -> R.drawable.ic_reader_webtoon_gap
    }
}