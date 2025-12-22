package com.shizq.bika.ui.reader.state

sealed interface ActiveSheet {
    data object None : ActiveSheet
    data object ReadingMode : ActiveSheet
    data object Orientation : ActiveSheet
}