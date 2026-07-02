package com.shizq.bika.ui.tag

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shizq.bika.core.model.SortOrder

@Composable
fun SortChip(
    currentSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
    allSortOptions: List<SortOrder> = listOf(
        SortOrder.NEWEST,
        SortOrder.MOST_VIEWED,
        SortOrder.MOST_LIKED,
        SortOrder.OLDEST
    )
) {
    var showDropdown by rememberSaveable { mutableStateOf(false) }
    val currentSortDisplayName = mapSortToDisplayName(currentSort)

    Box(modifier) {
        InputChip(
            selected = true,
            onClick = { showDropdown = true },
            label = { Text(text = currentSortDisplayName) },
            trailingIcon = {
                Icon(
                    Icons.Rounded.ArrowDropDown,
                    contentDescription = "选择排序方式",
                    modifier = Modifier.size(InputChipDefaults.IconSize),
                )
            },
        )

        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false }
        ) {
            for (sortOption in allSortOptions) {
                val isSelected = sortOption == currentSort
                val displayName = mapSortToDisplayName(sortOption)

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "$displayName (已选中)",
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.width(24.dp))
                            }
                            Text(text = displayName, modifier = Modifier.weight(1f))
                        }
                    },
                    onClick = {
                        onSortSelected(sortOption)
                        showDropdown = false
                    }
                )
            }
        }
    }
}

@Composable
private fun mapSortToDisplayName(sort: SortOrder): String {
    return when (sort) {
        SortOrder.NEWEST -> "最新排序"
        SortOrder.OLDEST -> "最早发布"
        SortOrder.MOST_LIKED -> "最多喜欢"
        SortOrder.MOST_VIEWED -> "最多浏览"
    }
}