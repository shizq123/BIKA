package com.shizq.bika.ui.reader.settings


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsSheetState: SheetState,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = settingsSheetState
    ) {
        SettingsContent()
    }
}

@Composable
fun SettingsContent() {
    var selectedMode by remember { mutableStateOf(ReadingMode.Strip) }
    var selectedOrientation by remember { mutableStateOf(ScreenOrientation.Portrait) }
    var selectedTouchArea by remember { mutableStateOf(TouchArea.Sides) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("阅读模式", isSubTitle = true)
                OptionFlowRow(
                    options = ReadingMode.entries,
                    selectedOption = selectedMode,
                    onOptionSelected = { selectedMode = it },
                    labelProvider = { it.label }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 组：屏幕方向
                SectionTitle("屏幕方向", isSubTitle = true)
                OptionFlowRow(
                    options = ScreenOrientation.entries,
                    selectedOption = selectedOrientation,
                    onOptionSelected = { selectedOrientation = it },
                    labelProvider = { it.label }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("点按区域", isSubTitle = true)
                OptionFlowRow(
                    options = TouchArea.entries,
                    selectedOption = selectedTouchArea,
                    onOptionSelected = { selectedTouchArea = it },
                    labelProvider = { it.label }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 标题组件
 */
@Composable
fun SectionTitle(text: String, isSubTitle: Boolean = false) {
    Text(
        text = text,
        style = if (isSubTitle) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
        color = if (isSubTitle) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * 自动换行的选项组 (核心布局)
 * 使用 M3 的 FlowRow
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> OptionFlowRow(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelProvider: (T) -> String
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { item ->
            SelectableChip(
                selected = item == selectedOption,
                text = labelProvider(item),
                onClick = { onOptionSelected(item) }
            )
        }
    }
}

/**
 * 单个选择 Chip (模拟图中的样式)
 * 选中时：浅紫色背景 + 深色文字
 * 未选中：透明/灰色背景 + 边框
 */
@Composable
fun SelectableChip(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }

    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (selected) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

enum class ReadingMode(val label: String) {
    //    SingleLR("单页式（从左到右）"),
//    SingleRL("单页式（从右到左）"),
//    SingleTB("单页式（从上到下）"),
    Strip("条漫"),
//    StripGap("条漫（页间有空隙）")
}

enum class ScreenOrientation(val label: String) {
    //    System("跟随系统"),
    Portrait("竖屏"),
//    Landscape("横屏"),
//    LockPortrait("锁定竖屏"),
//    LockLandscape("锁定横屏"),
//    ReversePortrait("反向竖屏")
}

enum class TouchArea(val label: String) {
    //    LShape("L 形"),
//    Kindle("Kindle"),
    Sides("两侧"),
//    LeftRight("左右"),
//    Off("关闭")
}

@Preview
@Composable
fun PreviewSettings() {
    MaterialTheme {
        SettingsContent()
    }
}