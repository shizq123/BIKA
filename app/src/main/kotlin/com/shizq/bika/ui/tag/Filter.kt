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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shizq.bika.R
import com.shizq.bika.core.domain.filter.ComicStatus
import com.shizq.bika.core.domain.filter.FeedCriteria
import com.shizq.bika.core.domain.filter.IntRangeSpec

/** 仅供 Compose 筛选控件使用的展示模型。 */
sealed interface FilterGroup {
    val label: String
    val options: List<FilterOption>

    data object Topic : FilterGroup {
        override val label = "主题"
        override val options = TOPIC_OPTIONS
    }

    data object ExcludeTopic : FilterGroup {
        override val label = "排除主题"
        override val options = TOPIC_OPTIONS
    }

    data object Status : FilterGroup {
        override val label = "状态"
        override val options = listOf(
            FilterOption.Status(finished = true, label = "完结"),
            FilterOption.Status(finished = false, label = "连载"),
        )
    }

    data object EpsRange : FilterGroup {
        override val label = "话数"
        override val options = listOf(
            countRange(1, 1, "单话 (1话)"),
            countRange(2, 5, "短篇 (2-5话)"),
            countRange(6, 20, "中篇 (6-20话)"),
            countRange(21, 100, "长篇 (21-100话)"),
            countRange(101, null, "超长篇 (100话以上)"),
        )
    }

    data object PagesRange : FilterGroup {
        override val label = "页数"
        override val options = listOf(
            countRange(1, 49, "少页 (<50页)"),
            countRange(50, 200, "中等 (50-200页)"),
            countRange(201, 500, "多页 (200-500页)"),
            countRange(501, null, "超多页 (500页以上)"),
        )
    }

    companion object {
        val all: List<FilterGroup> = listOf(Topic, ExcludeTopic, Status, EpsRange, PagesRange)
    }
}

sealed interface FilterOption {
    val label: String

    data class Topic(val name: String) : FilterOption {
        override val label: String get() = name
    }

    data class Status(
        val finished: Boolean,
        override val label: String,
    ) : FilterOption

    data class CountRange(
        val min: Int?,
        val max: Int?,
        override val label: String,
    ) : FilterOption
}

private fun countRange(min: Int?, max: Int?, label: String) =
    FilterOption.CountRange(min, max, label)

private fun customPagesRange(min: Int?, max: Int?): FilterOption.CountRange? {
    if (min == null && max == null) return null
    if (min != null && max != null && min > max) return null
    val label = when {
        min != null && max != null -> "指定数量: $min - $max 页"
        min != null -> "指定数量: >= $min 页"
        else -> "指定数量: <= $max 页"
    }
    return FilterOption.CountRange(min, max, label)
}

private val TOPIC_OPTIONS: List<FilterOption> = listOf(
    "全彩", "長篇", "同人", "短篇", "圓神領域", "碧藍幻想", "CG雜圖", "英語 ENG",
    "生肉", "純愛", "百合花園", "耽美花園", "偽娘哲學", "後宮閃光", "扶他樂園",
    "單行本", "姐姐系", "妹妹系", "SM", "性轉換", "足の恋", "人妻", "NTR", "強暴",
    "非人類", "艦隊收藏", "Love Live", "SAO 刀劍神域", "Fate", "東方", "WEBTOON",
    "禁書目錄", "歐美", "Cosplay", "重口地帶",
).map(FilterOption::Topic)

fun FeedCriteria.toFilterState(): FilterState = FilterState(
    chips = FilterGroup.all.map { group ->
        val selected = selectedOptions(group)
        FilterChipState(
            group = group,
            options = group.options + selected.filterNot { it in group.options },
            selected = selected,
        )
    },
)

private fun FeedCriteria.selectedOptions(group: FilterGroup): List<FilterOption> = when (group) {
    FilterGroup.Topic -> includedTopics.map(FilterOption::Topic)
    FilterGroup.ExcludeTopic -> excludedTopics.map(FilterOption::Topic)
    FilterGroup.Status -> statuses.map { status ->
        FilterOption.Status(
            finished = status.finished,
            label = if (status == ComicStatus.Finished) "完结" else "连载",
        )
    }

    FilterGroup.EpsRange -> episodeRanges.map { it.toOption(group) }
    FilterGroup.PagesRange -> pageRanges.map { it.toOption(group) }
}

private fun IntRangeSpec.toOption(group: FilterGroup): FilterOption.CountRange =
    group.options
        .filterIsInstance<FilterOption.CountRange>()
        .firstOrNull { it.min == min && it.max == max }
        ?: FilterOption.CountRange(
            min = min,
            max = max,
            label = when {
                min != null && max != null -> "指定数量: $min - $max"
                min != null -> "指定数量: >= $min"
                else -> "指定数量: <= $max"
            } + if (group == FilterGroup.PagesRange) " 页" else " 话",
        )

@Composable
fun rememberFilterState(criteria: FeedCriteria): FilterState = remember(criteria) {
    criteria.toFilterState()
}

@Immutable
data class FilterState(val chips: List<FilterChipState>)

sealed interface FeedFilterChange {
    data class IncludedTopic(val topic: String) : FeedFilterChange
    data class ExcludedTopic(val topic: String) : FeedFilterChange
    data class Status(val status: ComicStatus) : FeedFilterChange
    data class EpisodeRange(val range: IntRangeSpec) : FeedFilterChange
    data class PageRange(val range: IntRangeSpec) : FeedFilterChange
}

@Immutable
data class FilterChipState(
    val group: FilterGroup,
    val options: List<FilterOption>,
    val selected: List<FilterOption>,
) {
    val label: String get() = group.label
    val hasSelection: Boolean get() = selected.isNotEmpty()

    fun changeFor(option: FilterOption): FeedFilterChange = when {
        group is FilterGroup.Topic && option is FilterOption.Topic ->
            FeedFilterChange.IncludedTopic(option.name)

        group is FilterGroup.ExcludeTopic && option is FilterOption.Topic ->
            FeedFilterChange.ExcludedTopic(option.name)

        group is FilterGroup.Status && option is FilterOption.Status ->
            FeedFilterChange.Status(
                if (option.finished) ComicStatus.Finished else ComicStatus.Ongoing
            )

        group is FilterGroup.EpsRange && option is FilterOption.CountRange ->
            FeedFilterChange.EpisodeRange(IntRangeSpec(option.min, option.max))

        group is FilterGroup.PagesRange && option is FilterOption.CountRange ->
            FeedFilterChange.PageRange(IntRangeSpec(option.min, option.max))

        else -> error("Filter option $option does not belong to group $group")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChip(
    state: FilterChipState,
    onSelectionChanged: (FeedFilterChange) -> Unit,
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
                        text = stringResource(R.string.filter_select_title, state.label),
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
                            onClick = { onSelectionChanged(state.changeFor(option)) },
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
            text = stringResource(R.string.filter_global_enabled),
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

/** 自定义页数区间输入；区间校验在 UI 边界完成，领域层仍保持最终约束。 */
@Composable
private fun CustomRangeInput(onAdd: (FeedFilterChange) -> Unit) {
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
            label = stringResource(R.string.filter_min_pages),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.filter_range_separator),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.width(8.dp))
        DigitsField(
            value = maxText,
            onValueChange = { maxText = it },
            label = stringResource(R.string.filter_max_pages),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = {
                customPagesRange(minText.toIntOrNull(), maxText.toIntOrNull())
                    ?.let { range ->
                        onAdd(
                            FeedFilterChange.PageRange(
                                IntRangeSpec(
                                    range.min,
                                    range.max
                                )
                            )
                        )
                    }
                minText = ""
                maxText = ""
            },
            enabled = minText.isNotEmpty() || maxText.isNotEmpty(),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(stringResource(R.string.filter_add))
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

@Composable
private fun renderChipLabel(state: FilterChipState): String = when {
    !state.hasSelection -> state.label
    state.group is FilterGroup.ExcludeTopic -> stringResource(
        R.string.filter_exclude_prefix,
        state.selected.joinToString(",") { it.label },
    )

    else -> state.selected.joinToString(",") { it.label }
}
