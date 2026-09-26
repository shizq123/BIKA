package com.shizq.bika.ui.tag

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.domain.filter.FilterGroup
import com.shizq.bika.core.domain.filter.FilterOption
import com.shizq.bika.core.domain.filter.FilterSelections
import com.shizq.bika.core.domain.filter.customPagesRange

@Composable
fun rememberFilterState(selections: FilterSelections): FilterState = remember(selections) {
    FilterState(
        chips = FilterGroup.all.map { group ->
            val selected = selections[group].orEmpty()
            FilterChipState(
                group = group,
                // 用户自定义区间不在 group.options 里，需并入以便渲染出可取消的选项
                options = group.options + selected.filterNot { it in group.options },
                selected = selected,
            )
        },
    )
}

@Immutable
data class FilterState(val chips: List<FilterChipState>)

@Immutable
data class FilterChipState(
    val group: FilterGroup,
    val options: List<FilterOption>,
    val selected: List<FilterOption>,
) {
    val label: String get() = group.label
    val hasSelection: Boolean get() = selected.isNotEmpty()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChip(
    state: FilterChipState,
    onSelectionChanged: (FilterOption) -> Unit,
    excludeTopicsGlobal: Boolean = false,
    onExcludeTopicsGlobalChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

    Box(modifier) {
        InputChip(
            selected = state.hasSelection,
            onClick = { showSheet = true },
            label = {
                Text(
                    text = renderChipLabel(state),
                    modifier = Modifier.widthIn(max = 160.dp),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            },
            trailingIcon = {
                Icon(
                    Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(InputChipDefaults.IconSize),
                )
            },
        )

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp),
                ) {
                    Text(
                        text = "选择${state.label}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp),
                    )

                    if (state.group is FilterGroup.ExcludeTopic) {
                        GlobalToggleRow(
                            enabled = excludeTopicsGlobal,
                            onEnabledChange = onExcludeTopicsGlobalChanged,
                        )
                    }

                    state.options.forEach { option ->
                        OptionRow(
                            label = option.label,
                            checked = option in state.selected,
                            onClick = { onSelectionChanged(option) },
                        )
                    }

                    if (state.group is FilterGroup.PagesRange) {
                        CustomRangeInput(onAdd = onSelectionChanged)
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalToggleRow(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEnabledChange(!enabled) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "全局生效",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun OptionRow(label: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** 自定义页数区间输入。label 由 domain 层的 customPagesRange 生成，此处不拼接文案。 */
@Composable
private fun CustomRangeInput(onAdd: (FilterOption) -> Unit) {
    var minText by remember { mutableStateOf("") }
    var maxText by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DigitsField(
            value = minText,
            onValueChange = { minText = it },
            label = "最少页数",
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text("至", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        DigitsField(
            value = maxText,
            onValueChange = { maxText = it },
            label = "最多页数",
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = {
                customPagesRange(minText.toIntOrNull(), maxText.toIntOrNull())?.let(onAdd)
                minText = ""
                maxText = ""
            },
            enabled = minText.isNotEmpty() || maxText.isNotEmpty(),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text("添加")
        }
    }
}

@Composable
private fun DigitsField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> if (text.all { it.isDigit() }) onValueChange(text) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        singleLine = true,
    )
}

private fun renderChipLabel(state: FilterChipState): String = when {
    !state.hasSelection -> state.label
    state.group is FilterGroup.ExcludeTopic ->
        "排除: " + state.selected.joinToString(",") { it.label }

    else -> state.selected.joinToString(",") { it.label }
}
