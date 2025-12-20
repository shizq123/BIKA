package com.shizq.bika.ui.reader.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shizq.bika.R
import com.shizq.bika.core.model.ReadingMode
import com.shizq.bika.ui.reader.settings.OptionFlowRow

@Composable
fun ReadingModeSelectBottomSheet(
    activeMode: ReadingMode,
    onReadingModeChanged: (ReadingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    ModeSelectionBottomSheet(
        onDismissRequest = {},
        onApply = { onReadingModeChanged(activeMode) },
        title = { Text("阅读模式") },
        modifier = modifier,
    ) {
        OptionFlowRow(
            options = ReadingMode.entries,
            selectedOption = activeMode,
            onOptionSelected = onReadingModeChanged,
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