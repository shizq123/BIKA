package com.shizq.bika.ui.feed

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.shizq.bika.R

@Composable
fun FavoriteTagNameDialog(
    title: String,
    label: String,
    confirmText: String,
    initialValue: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember(initialValue) { mutableStateOf(initialValue) }
    val normalizedInput = normalizeFavoriteTagName(input)

    fun submit() {
        normalizedInput?.let(onConfirm)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(FavoriteTagNameTestTags.INPUT),
            )
        },
        confirmButton = {
            TextButton(
                enabled = normalizedInput != null,
                onClick = { submit() },
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.feed_dialog_cancel))
            }
        },
    )
}

internal object FavoriteTagNameTestTags {
    const val INPUT = "favorite-tag-name-input"
}

internal fun normalizeFavoriteTagName(input: String): String? =
    input.trim().takeIf(String::isNotEmpty)
