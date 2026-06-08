package com.shizq.bika.ui.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.model.TapZoneLayout
import com.shizq.bika.core.model.BookSpreadsMode
import com.shizq.bika.core.model.ReadingMode
import com.shizq.bika.ui.reader.layout.ReaderConfig
import com.shizq.bika.ui.reader.state.ReaderAction
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsBottomSheet(
    config: ReaderConfig,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dispatch: (ReaderAction) -> Unit,
) {
    BottomSheet(
        title = "阅读设置",
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
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
                        .padding(vertical = 8.dp)
                ) {
                    SectionTitle("预加载图片数量")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var localPreloadCount by remember { mutableFloatStateOf(config.preloadCount.toFloat()) }
                        Slider(
                            value = localPreloadCount,
                            onValueChange = { newValue ->
                                localPreloadCount = newValue
                            },
                            onValueChangeFinished = {
                                dispatch(
                                    ReaderAction.SetPreloadCount(
                                        localPreloadCount.roundToInt()
                                    )
                                )
                            },
                            valueRange = 1f..16f,
                            // steps 计算公式：(max - min) - 1。这里 (16 - 1) - 1 = 14
                            steps = 14,
                            modifier = Modifier.weight(1f)
                        )

                        // 右侧显示当前数值
                        Text(
                            text = "${config.preloadCount}张",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .width(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    SectionTitle("点按区域")
                    OptionFlowRow(
                        options = TapZoneLayout.entries,
                        selectedOption = config.tapZoneLayout,
                        onOptionSelected = { dispatch(ReaderAction.SetTapZoneLayout(it)) },
                        labelProvider = { it.label }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    SectionTitle("交互控制")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .toggleable(
                                config.volumeKeyNavigation,
                                role = Role.Switch
                            ) {
                                dispatch(ReaderAction.SetVolumeKeyNavigation(it))
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
                            checked = config.volumeKeyNavigation,
                            onCheckedChange = null
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    SectionTitle("护眼与暗化蒙层")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .toggleable(
                                config.eyeCareEnabled,
                                role = Role.Switch
                            ) {
                                dispatch(ReaderAction.SetEyeCareEnabled(it))
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "夜间护眼蒙层",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "在最顶层加盖黑色透明滤镜，削减背景刺眼亮度",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.eyeCareEnabled,
                            onCheckedChange = null
                        )
                    }

                    if (config.eyeCareEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var localDarkness by remember(config.eyeCareDarkness) { mutableFloatStateOf(config.eyeCareDarkness) }
                            Slider(
                                value = localDarkness,
                                onValueChange = { localDarkness = it },
                                onValueChangeFinished = {
                                    dispatch(ReaderAction.SetEyeCareDarkness(localDarkness))
                                },
                                valueRange = 0.1f..0.6f,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(localDarkness * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .width(50.dp)
                            )
                        }
                    }

                    if (config.readingMode == ReadingMode.WEBTOON) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionTitle("条漫自动滚动")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .toggleable(
                                    config.autoScrollEnabled,
                                    role = Role.Switch
                                ) {
                                    dispatch(ReaderAction.SetAutoScrollEnabled(it))
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "自动滚动",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "条漫模式下一键启用自动向下慢滚页面",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = config.autoScrollEnabled,
                                onCheckedChange = null
                            )
                        }

                        if (config.autoScrollEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var localSpeed by remember(config.autoScrollSpeed) { mutableFloatStateOf(config.autoScrollSpeed.toFloat()) }
                                Slider(
                                    value = localSpeed,
                                    onValueChange = { localSpeed = it },
                                    onValueChangeFinished = {
                                        dispatch(ReaderAction.SetAutoScrollSpeed(localSpeed.roundToInt()))
                                    },
                                    valueRange = 1f..5f,
                                    steps = 3,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "速度 ${localSpeed.roundToInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier
                                        .padding(start = 16.dp)
                                        .width(60.dp)
                                )
                            }
                        }
                    }

                    if (config.readingMode != ReadingMode.WEBTOON) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionTitle("大屏版面")
                        OptionFlowRow(
                            options = BookSpreadsMode.entries,
                            selectedOption = config.bookSpreadsMode,
                            onOptionSelected = { dispatch(ReaderAction.SetBookSpreadsMode(it)) },
                            labelProvider = { it.label }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}