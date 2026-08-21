package com.shizq.bika.feature.reader.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.shizq.bika.feature.reader.impl.components.ReadingModeSelectBottomSheet
import com.shizq.bika.feature.reader.impl.components.ReadingSettingsBottomSheet
import com.shizq.bika.feature.reader.impl.components.ScreenOrientationSelectBottomSheet
import com.shizq.bika.feature.reader.impl.layout.ReaderConfig
import com.shizq.bika.feature.reader.impl.state.ReaderAction
import com.shizq.bika.feature.reader.impl.state.ReaderSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderSheetHost(
    sheet: ReaderSheet,
    config: ReaderConfig,
    dispatch: (ReaderAction) -> Unit,
) {
    val onClose = { dispatch(ReaderAction.HideSheet) }
    when (sheet) {
        ReaderSheet.ReadingMode -> {
            ReadingModeSelectBottomSheet(
                activeMode = config.readingMode,
                onReadingModeChanged = { dispatch(ReaderAction.SetReadingMode(it)) },
                onDismissRequest = onClose,
            )
        }

        ReaderSheet.Orientation -> {
            ScreenOrientationSelectBottomSheet(
                orientation = config.screenOrientation,
                onOrientationChange = { dispatch(ReaderAction.SetOrientation(it)) },
                onDismissRequest = onClose,
            )
        }

        ReaderSheet.Settings -> {
            ReadingSettingsBottomSheet(
                config = config,
                dispatch = dispatch,
                onDismissRequest = onClose,
            )
        }

        else -> Unit
    }
}
