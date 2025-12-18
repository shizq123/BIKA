package com.shizq.bika.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PreferenceGroup(
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            val primary = MaterialTheme.colorScheme.primary
            val titleStyle = MaterialTheme.typography.titleSmall.copy(color = primary)

            Box(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
                ProvideTextStyle(value = titleStyle) {
                    title()
                }
            }
        }
        content()
    }
}

@Composable
fun Preference(
    title: String,
    summary: String? = null,
    iconVector: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (summary != null) {
            { Text(summary) }
        } else null,
        leadingContent = if (iconVector != null) {
            { Icon(imageVector = iconVector, contentDescription = null) }
        } else null,
        modifier = Modifier.clickable(enabled = onClick != null) { onClick?.invoke() }
    )
}

@Composable
fun SwitchPreference(
    title: String,
    summary: String? = null,
    iconVector: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (summary != null) {
            { Text(summary) }
        } else null,
        leadingContent = if (iconVector != null) {
            { Icon(imageVector = iconVector, contentDescription = null) }
        } else null,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

/**
 * @param T 选项的数据类型。
 * @param title 偏好设置的标题。
 * @param selectedValue 当前选中的值。
 * @param options 所有可选项的列表。
 * @param onOptionSelected 当用户选择一个新选项时触发的回调。
 * @param modifier Modifier for this composable.
 * @param iconVector (可选) 显示在标题前的图标。
 * @param optionToText (可选) 一个将类型 T 转换为显示字符串的函数。
 *                     默认为调用 .toString()。
 */
@Composable
fun <T> ListPreference(
    title: String,
    selectedValue: T,
    options: List<T>,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    iconVector: ImageVector? = null,
    optionToText: (T) -> String = { it.toString() }
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(optionToText(selectedValue)) },
        leadingContent = if (iconVector != null) {
            { Icon(imageVector = iconVector, contentDescription = null) }
        } else null,
        modifier = modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column(Modifier.selectableGroup()) {
                    options.forEach { option ->
                        val isSelected = (option == selectedValue)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = isSelected,
                                    onClick = {
                                        onOptionSelected(option)
                                        showDialog = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = optionToText(option),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
@Preview
@Composable
fun PreferenceGroupPreview() {
    PreferenceGroup(
        title = { Text("Sample Preference Group") },
        content = {}
    )
}

@Preview
@Composable
fun PreferencePreview() {
    Preference(
        title = "Sample Preference",
        summary = "This is a sample summary."
    )
}

@Preview
@Composable
fun SwitchPreferencePreview() {
    SwitchPreference(
        title = "Sample Switch Preference",
        summary = "This is a sample summary.",
        checked = true,
        onCheckedChange = {}
    )
}

@Preview
@Composable
fun ListPreferencePreview() {
    ListPreference(
        title = "Sample List Preference",
        options = listOf("Option 1", "Option 2", "Option 3"),
        selectedValue = "Option 1",
        onOptionSelected = {}
    )
}