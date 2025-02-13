/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.expense

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.zu.chrimametro.ui.theme.ChrimametroTheme

@SuppressLint("DefaultLocale")
@Composable
fun ExpenseScreen(viewModel: MainViewmodel) {
    val message = remember { mutableStateOf("Edit Me") }

    val openDialog = remember { mutableStateOf(false) }
    val editMessage = remember { mutableStateOf("999") }

    val currentName = remember { mutableStateOf("") }

    val myState by viewModel.myStateFlow.collectAsState(emptyList())

    Box {
        LazyColumn {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth() // Ensure the row takes up the full width
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, // Distribute space evenly between the buttons,
                    verticalAlignment = Alignment.CenterVertically // Center the content vertically
                ) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "Average expense :" + String.format("%.2f", viewModel.expenseAverage()) + " ₣",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    FloatingActionButton(
                        onClick = { viewModel.addNextMonth() },
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Sort",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
            items(myState, key = { it.name }) { month ->
                //  SwipeCard(onDelete = { viewModel.onDeleteMonth(month) }) {
                MonthlyCard(
                    model = month, addEntry = openDialog,
                    elementClicked = { currentName.value = month.name },
                    viewModel
                )
                //  }
            }
        }

        if (openDialog.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = contentColorFor(MaterialTheme.colorScheme.background).copy(alpha = 0.6f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            openDialog.value = false
                        }), contentAlignment = Alignment.Center
            ) {
                CustomDialog(message, openDialog, editMessage, viewModel, currentName.value)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MonthlyCardPreview() {
    ChrimametroTheme {
        MonthlyCard(MonthWithdrawModel("Gen 23", mutableListOf(999.9f, 900.9f)), remember {
            mutableStateOf(false)
        }, {}, null)
    }
}

@Composable
fun CustomDialog(
    message: MutableState<String>,
    openDialog: MutableState<Boolean>,
    editMessage: MutableState<String>,
    viewModel: MainViewmodel,
    nameToStore: String?
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(text = "Insert The moneisian")

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = editMessage.value,
                onValueChange = { editMessage.value = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.align(Alignment.End)
        ) {
            Button(onClick = {
                openDialog.value = false
            }) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                message.value = editMessage.value
                openDialog.value = false
                nameToStore?.let {
                    viewModel.storeInput(it, editMessage.value.toFloat())
                }
            }) {
                Text("OK")
            }
        }
    }
}
