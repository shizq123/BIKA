package com.shizq.bika.ui.dashboard

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun CheckInResultDialog(
    result: CheckInResult,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        },
        title = { Text("打哔咔提示") },
        text = { Text(result.message) },
    )
}
