package com.shizq.bika.ui.reader

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.shizq.bika.ui.reader.components.ReadingModeSelectBottomSheet
import com.shizq.bika.ui.reader.components.ReadingSettingsBottomSheet
import com.shizq.bika.ui.reader.components.ScreenOrientationSelectBottomSheet
import com.shizq.bika.ui.reader.layout.ReaderConfig
import com.shizq.bika.ui.reader.state.ReaderAction
import com.shizq.bika.ui.reader.state.ReaderAction.HideSheet
import com.shizq.bika.ui.reader.state.ReaderAction.SetOrientation
import com.shizq.bika.ui.reader.state.ReaderAction.SetReadingMode
import com.shizq.bika.ui.reader.state.ReaderSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderSheetHost(
    sheet: ReaderSheet,
    config: ReaderConfig,
    dispatch: (ReaderAction) -> Unit,
) {
    val onClose = { dispatch(HideSheet) }
    when (sheet) {
        ReaderSheet.ReadingMode -> {
            ReadingModeSelectBottomSheet(
                activeMode = config.readingMode,
                onReadingModeChanged = { dispatch(SetReadingMode(it)) },
                onDismissRequest = onClose,
            )
        }

        ReaderSheet.Orientation -> {
            ScreenOrientationSelectBottomSheet(
                orientation = config.screenOrientation,
                onOrientationChange = { dispatch(SetOrientation(it)) },
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
