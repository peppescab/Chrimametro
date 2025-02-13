/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.tools

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeCard(
    isEnabled: Boolean = true,
    onDelete: () -> Unit,
    cardContent: @Composable () -> Unit
) {

    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onDelete()
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    return@rememberSwipeToDismissBoxState false
                }

                SwipeToDismissBoxValue.Settled -> {
                    return@rememberSwipeToDismissBoxState false
                }
            }
            return@rememberSwipeToDismissBoxState true
        }

    )
    if (isEnabled) {
        SwipeToDismissBox(
            modifier = Modifier.animateContentSize(),
            state = swipeState,
            backgroundContent = {

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "delete"
                    )
                }
            },
            content = { cardContent() }
        )
    } else {
        cardContent()
    }
}
