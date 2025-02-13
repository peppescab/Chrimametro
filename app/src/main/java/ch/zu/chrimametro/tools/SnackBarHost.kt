/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.tools

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// SnackbarHost to display the Snackbar
@Composable
fun BoxScope.SnackbarCustom(
    isDimissable: Boolean = true,
    snackbarHostState: SnackbarHostState,
    onClicked: () -> Unit
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
    ) { snackbarData ->
        Snackbar(
            action = {
                Row(modifier = Modifier.size(64.dp)) {
                    snackbarData.visuals.actionLabel?.let { actionLabel ->
                        TextButton(onClick = {
                            onClicked()
                            if (isDimissable)
                                snackbarHostState.currentSnackbarData?.dismiss()
                        }) {
                            Text(actionLabel)
                        }
                    }
                }
            }
        ) {
            Text(snackbarData.visuals.message)
        }
    }
}
