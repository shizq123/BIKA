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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
        val filterGroups = listOf(FilterGroup.Topic, FilterGroup.ExcludeTopic, FilterGroup.Status)
        val chips = filterGroups.map { group ->

            val currentSelection = selections[group].orEmpty()

            val (label, values) = when (group) {
                is FilterGroup.Topic -> "主题" to group.values
                is FilterGroup.ExcludeTopic -> "排除主题" to group.values
                is FilterGroup.Status -> "状态" to group.values
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
    var showSheet by rememberSaveable { mutableStateOf(false) }
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
