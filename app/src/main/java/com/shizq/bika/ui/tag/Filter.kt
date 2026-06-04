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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun rememberFilterState(
    selections: Map<FilterGroup, List<String>>
): FilterState {
    return remember(selections) {
        val filterGroups = listOf(
            FilterGroup.Topic,
            FilterGroup.ExcludeTopic,
            FilterGroup.Status,
            FilterGroup.EpsRange,
            FilterGroup.PagesRange,
        )
        val chips = filterGroups.map { group ->

            val currentSelection = selections[group].orEmpty()

            val (label, values) = when (group) {
                is FilterGroup.Topic -> "主题" to group.values
                is FilterGroup.ExcludeTopic -> "排除主题" to group.values
                is FilterGroup.Status -> "状态" to group.values
                is FilterGroup.EpsRange -> "话数" to group.values
                is FilterGroup.PagesRange -> "页数" to group.values
            }
            FilterChipState(
                label = label,
                values = values,
                selected = currentSelection,
                kind = group
            )
        }
        FilterState(chips = chips)
    }
}

@Immutable
data class FilterState(
    val chips: List<FilterChipState>,
)

@Immutable
data class FilterChipState(
    val label: String,
    val values: List<String>,
    val selected: List<String>,
    val kind: FilterGroup? = null,
) {
    val hasSelection: Boolean
        get() = selected.isNotEmpty()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChip(
    state: FilterChipState,
    onSelectionChanged: (value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

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
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "选择${state.label}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    state.values.forEach { value ->
                        val isSelected = value in state.selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectionChanged(value) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(text = value, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    if (state.kind is FilterGroup.PagesRange) {
                        var customCountText by remember { mutableStateOf("") }
                        val currentCustomSelections = state.selected.filter { it.startsWith("指定数量: ") }

                        if (currentCustomSelections.isNotEmpty()) {
                            Text(
                                text = "已指定数量",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        currentCustomSelections.forEach { customVal ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectionChanged(customVal) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = true,
                                    onCheckedChange = null
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(text = customVal, style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        var minCountText by remember { mutableStateOf("") }
                        var maxCountText by remember { mutableStateOf("") }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = minCountText,
                                onValueChange = { text ->
                                    if (text.all { it.isDigit() }) {
                                        minCountText = text
                                    }
                                },
                                label = { Text("最少页数") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("至", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = maxCountText,
                                onValueChange = { text ->
                                    if (text.all { it.isDigit() }) {
                                        maxCountText = text
                                    }
                                },
                                label = { Text("最多页数") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val min = minCountText.trim().toIntOrNull()
                                    val max = maxCountText.trim().toIntOrNull()
                                    if (min != null || max != null) {
                                        val label = when {
                                            min != null && max != null -> "指定数量: $min - $max 页"
                                            min != null -> "指定数量: >= $min 页"
                                            else -> "指定数量: <= $max 页"
                                        }
                                        if (label !in state.selected) {
                                            onSelectionChanged(label)
                                        }
                                        minCountText = ""
                                        maxCountText = ""
                                    }
                                },
                                enabled = minCountText.trim().isNotEmpty() || maxCountText.trim().isNotEmpty(),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text("添加")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun renderChipLabel(state: FilterChipState): String {
    return if (state.hasSelection) {
        if (state.kind is FilterGroup.ExcludeTopic) {
            "排除: " + state.selected.joinToString(",")
        } else {
            state.selected.joinToString(",")
        }
    } else {
        state.label
    }
}
