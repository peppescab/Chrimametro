/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.zu.chrimametro.ui.theme.ChrimametroTheme

@Composable
fun MainComposable(monthsList: List<MonthWithdrawModel>, viewModel: MainViewmodel) {
    val message = remember { mutableStateOf("Edit Me") }

    val openDialog = remember { mutableStateOf(false) }
    val editMessage = remember { mutableStateOf("") }

    val currentName = remember { mutableStateOf("") }

    Box {
        LazyColumn {
            items(monthsList) { month ->
                MonthlyCard(
                    model = month, addEntry = openDialog,
                    elementClicked = { currentName.value = month.name },
                    viewModel
                )
            }
        }

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            onClick = { viewModel.addNewMonth() }
        ) {
            Icon(Icons.Filled.Add, "Floating action button.")
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

@Composable
fun MonthlyCard(
    model: MonthWithdrawModel, addEntry: MutableState<Boolean>, elementClicked: (String) -> Unit,
    viewModel: MainViewmodel?
) {
    ChrimametroTheme {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = model.getTotal().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.padding(2.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(), shape = MaterialTheme.shapes.medium
            ) {
                Spacer(modifier = Modifier.padding(4.dp))
                model.expenses.forEach { expense ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = expense.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .weight(1f)
                        )
                        Icon(
                            Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        viewModel?.deleteEntry(model.name, expense)
                                    })
                        )
                        Spacer(modifier = Modifier.padding(24.dp))

                    }
                }

                Row {
                    FloatingActionButton(
                        onClick = {
                            addEntry.value = true
                            elementClicked(model.name)
                        }, modifier = Modifier
                            .padding(16.dp)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }

                    FloatingActionButton(
                        onClick = {

                        }, modifier = Modifier
                            .padding(16.dp)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Add")
                    }
                }

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
