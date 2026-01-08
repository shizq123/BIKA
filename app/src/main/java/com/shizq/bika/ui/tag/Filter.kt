package com.shizq.bika.ui.tag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun rememberFilterState(
    selections: Map<FilterGroup, List<String>>,
    dynamicTags: List<String>
): FilterState {
    val tagGroup = FilterGroup.Tag(dynamicTags)
    val filterGroups = listOf(FilterGroup.Topic, FilterGroup.Status, tagGroup)

    val chips = filterGroups.map { group ->
        val currentSelection = selections.getOrDefault(group, emptyList())

        val label = when (group) {
            is FilterGroup.Topic -> "主题"
            is FilterGroup.Status -> "状态"
            is FilterGroup.Tag -> "标签"
        }

        val values = when (group) {
            is FilterGroup.Topic -> group.values
            is FilterGroup.Status -> group.values
            is FilterGroup.Tag -> dynamicTags
        }

        FilterChipState(
            label = label,
            values = values,
            selected = currentSelection,
            kind = group
        )
    }

    return FilterState(chips = chips)
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

@Composable
fun FilterChipsRow(
    state: FilterState,
    onSelectionChanged: (chipState: FilterChipState, value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (chipState in state.chips) {
            FilterChip(
                state = chipState,
                onSelectionChanged = { value -> onSelectionChanged(chipState, value) }
            )
        }
    }
}

@Composable
fun FilterChip(
    state: FilterChipState,
    onSelectionChanged: (value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDropdown by rememberSaveable { mutableStateOf(false) }

    Box(modifier) {
        InputChip(
            selected = state.hasSelection,
            onClick = { showDropdown = true },
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

        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false }
        ) {
            for (value in state.values) {
                val isSelected = value in state.selected
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
                            )
                            Text(text = value, modifier = Modifier.weight(1f))
                        }
                    },
                    onClick = {
                        onSelectionChanged(value)
                    }
                )
            }
        }
    }
}

private fun renderChipLabel(state: FilterChipState): String {
    return if (state.hasSelection) {
        state.selected.joinToString(",")
    } else {
        state.label
    }
}
