package com.shizq.bika.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PreferenceGroup(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

/**
 * 普通点击项 (对应 Preference)
 */
@Composable
fun RegularPreference(
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
            {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        modifier = Modifier.clickable(enabled = onClick != null) { onClick?.invoke() },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    )
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
                onCheckedChange = null // null 因为点击整个 Item 也会触发
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
fun ListPreference(
    title: String,
    iconVector: ImageVector? = null,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(options.getOrElse(selectedIndex) { "" }) },
        leadingContent = if (iconVector != null) {
            { Icon(imageVector = iconVector, contentDescription = null) }
        } else null,
        modifier = Modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column(Modifier.selectableGroup()) {
                    options.forEachIndexed { index, text ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (index == selectedIndex),
                                    onClick = {
                                        onOptionSelected(index)
                                        showDialog = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (index == selectedIndex),
                                onClick = null
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(text = text, style = MaterialTheme.typography.bodyLarge)
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
    PreferenceGroup(title = "Sample Preference Group")
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
        selectedIndex = 1,
        onOptionSelected = {}
    )
}