package com.shizq.bika.ui.reader.settings


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shizq.bika.core.model.ReadingMode
import com.shizq.bika.core.model.ScreenOrientation
import com.shizq.bika.core.model.TapZoneLayout
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    settingsSheetState: SheetState,
    onDismissRequest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = settingsSheetState
    ) {
        SettingsContent(
            uiState = uiState,
            onReadingModeChanged = viewModel::updateReadingMode,
            onScreenOrientationChanged = viewModel::updateScreenOrientation,
            onTapZoneLayoutChanged = viewModel::setTapZoneLayout,
            onVolumeKeyNavigationChanged = viewModel::setVolumeKeyNavigation,
            onPreloadCountChanged = viewModel::updatePreloadCount

        )
    }
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onReadingModeChanged: (ReadingMode) -> Unit = {},
    onScreenOrientationChanged: (ScreenOrientation) -> Unit = {},
    onTapZoneLayoutChanged: (TapZoneLayout) -> Unit = {},
    onVolumeKeyNavigationChanged: (Boolean) -> Unit = {},
    onPreloadCountChanged: (Int) -> Unit = {},
) {
    when (uiState) {
        SettingsUiState.Loading -> {}
        is SettingsUiState.Success -> {
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
                            selectedOption = uiState.userData.readingMode,
                            onOptionSelected = onReadingModeChanged,
                            labelProvider = { it.label }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        SectionTitle("屏幕方向", isSubTitle = true)
                        OptionFlowRow(
                            options = ScreenOrientation.entries,
                            selectedOption = uiState.userData.screenOrientation,
                            onOptionSelected = onScreenOrientationChanged,
                            labelProvider = { it.label }
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        SectionTitle("预加载图片数量", isSubTitle = true)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = uiState.userData.preloadCount.toFloat(),
                                onValueChange = { newValue ->
                                    onPreloadCountChanged(newValue.roundToInt())
                                },
                                valueRange = 1f..16f,
                                // steps 计算公式：(max - min) - 1。这里 (16 - 1) - 1 = 14
                                steps = 14,
                                modifier = Modifier.weight(1f)
                            )

                            // 右侧显示当前数值
                            Text(
                                text = "${uiState.userData.preloadCount}张",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .width(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        SectionTitle("点按区域", isSubTitle = true)
                        OptionFlowRow(
                            options = TapZoneLayout.entries,
                            selectedOption = uiState.userData.tapZoneLayout,
                            onOptionSelected = onTapZoneLayoutChanged,
                            labelProvider = { it.label }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        SectionTitle("交互控制", isSubTitle = true)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    onVolumeKeyNavigationChanged(!uiState.userData.volumeKeyNavigation)
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "音量键翻页",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "使用物理音量键切换上一页/下一页",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.userData.volumeKeyNavigation,
                                onCheckedChange = onVolumeKeyNavigationChanged
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
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