package com.shizq.bika.ui.feed

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.shizq.bika.R

@Composable
internal fun FeedPageJumpDialog(
    currentPage: Int,
    totalPages: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable(currentPage, stateSaver = TextFieldValue.Saver) {
        val text = currentPage.toString()
        mutableStateOf(TextFieldValue(text, selection = TextRange(0, text.length)))
    }
    val focusRequester = remember { FocusRequester() }
    val targetPage = parseFeedPageNumber(input.text, totalPages)
    val hasError = input.text.isNotBlank() && targetPage == null

    fun submit() {
        targetPage?.let(onConfirm)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feed_jump_page_title)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.feed_jump_page_label)) },
                supportingText = {
                    Text(
                        stringResource(
                            if (hasError) R.string.feed_jump_page_error else R.string.feed_jump_page_range,
                            totalPages,
                        ),
                    )
                },
                isError = hasError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag(FeedTestTags.PageInput),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { submit() },
                enabled = targetPage != null,
                modifier = Modifier.testTag(FeedTestTags.JumpConfirm),
            ) {
                Text(stringResource(R.string.feed_jump_page_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.feed_dialog_cancel))
            }
        },
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

internal fun parseFeedPageNumber(input: String, totalPages: Int): Int? {
    val text = input.trim()
    if (text.isEmpty() || text.any { it !in '0'..'9' }) return null
    return text.toIntOrNull()?.takeIf { it in 1..totalPages }
}
