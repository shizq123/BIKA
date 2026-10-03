package com.shizq.bika.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shizq.bika.R

/** 由 [com.shizq.bika.navigation.FeedPageJumpDialogNavKey] 承载的页码输入弹窗。 */
@Composable
fun FeedPageJumpDialog(
    currentPage: Int,
    totalPages: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember(currentPage) { mutableStateOf(currentPage.toString()) }
    var validationError by remember { mutableStateOf<PageJumpValidationError?>(null) }

    fun submit() {
        val result = validatePageJump(input, totalPages)
        validationError = result.error
        result.page?.let(onConfirm)
    }

    val errorMessage = when (validationError) {
        PageJumpValidationError.InvalidNumber -> stringResource(R.string.feed_page_jump_invalid_number)
        PageJumpValidationError.BelowMinimum -> stringResource(R.string.feed_page_jump_below_min)
        PageJumpValidationError.AboveMaximum -> stringResource(
            R.string.feed_page_jump_above_max,
            totalPages,
        )

        null -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feed_page_jump_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.feed_page_jump_hint, totalPages),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        validationError = null
                    },
                    label = { Text(stringResource(R.string.feed_page_jump_label)) },
                    singleLine = true,
                    isError = validationError != null,
                    supportingText = errorMessage?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { submit() }),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }) {
                Text(stringResource(R.string.feed_page_jump_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.feed_page_jump_cancel))
            }
        },
    )
}

internal enum class PageJumpValidationError {
    InvalidNumber,
    BelowMinimum,
    AboveMaximum,
}

internal data class PageJumpValidationResult(
    val page: Int? = null,
    val error: PageJumpValidationError? = null,
)

internal fun validatePageJump(
    input: String,
    totalPages: Int,
): PageJumpValidationResult {
    val targetPage = input.trim().toIntOrNull()
        ?: return PageJumpValidationResult(error = PageJumpValidationError.InvalidNumber)
    return when {
        targetPage < 1 -> PageJumpValidationResult(error = PageJumpValidationError.BelowMinimum)
        targetPage > totalPages.coerceAtLeast(1) ->
            PageJumpValidationResult(error = PageJumpValidationError.AboveMaximum)

        else -> PageJumpValidationResult(page = targetPage)
    }
}
