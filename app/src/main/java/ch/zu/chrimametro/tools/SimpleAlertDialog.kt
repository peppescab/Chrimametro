/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.tools

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SimpleAlertDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            confirmButton = {
                Button(onClick = {
                    onConfirm()
                    onDismiss()
                }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                Button(onClick = { onDismiss() }) {
                    Text("Cancel")
                }
            },
            title = {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        )
    }
}


/*
currentMonthBudgetModel?.let {
    viewModel.onDeleteItem(it)
    deleteMonth = false
}

"Do you want to remove " + currentMonthBudgetModel?.nameMonth
*/

