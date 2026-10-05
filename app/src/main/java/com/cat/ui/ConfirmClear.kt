package com.cat.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonMagenta

/** v1.16: one confirm dialog for every destructive clear. */
@Composable
fun ConfirmClearDialog(
    title: String,
    body: String,
    confirmLabel: String = "Clear",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmLabel, color = NeonMagenta) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = NeonCyan) }
        }
    )
}
