/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.expense

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ch.zu.chrimametro.tools.SingleTextDialog
import ch.zu.chrimametro.R
import ch.zu.chrimametro.ui.getExpensesBackground
import ch.zu.chrimametro.ui.theme.ChrimametroTheme

@Composable
fun MonthlyCard(
    model: MonthWithdrawModel,
    addEntry: MutableState<Boolean>,
    elementClicked: (String) -> Unit,
    viewModel: MainViewmodel?
) {
    val showDialog = remember { mutableStateOf(false) }
    val noteText = remember { mutableStateOf("") }

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
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.medium,
                colors = CardColors(
                    containerColor = getExpensesBackground(model.getTotal()),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContentColor = Color.White,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Spacer(modifier = Modifier.height(4.dp))
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
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            viewModel?.deleteEntry(model.name, expense)
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(24.dp))
                        }
                    }
                    HorizontalDivider(color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    model.listNote.forEach { note ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .weight(1f)
                            )
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            viewModel?.deleteNote(model.name, note)
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(24.dp))
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FloatingActionButton(
                            onClick = {
                                addEntry.value = true
                                elementClicked(model.name)
                            },
                            modifier = Modifier
                                .padding(16.dp)
                                .size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }

                        FloatingActionButton(
                            onClick = {
                                showDialog.value = true
                            },
                            modifier = Modifier
                                .padding(16.dp)
                                .size(48.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_notes), contentDescription = "Edit",
                                contentScale = ContentScale.FillBounds, modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    SingleTextDialog(
        showDialog = showDialog,
        textToShow = noteText.value,
        onNoteChange = { noteText.value = it },
        onSave = {
            viewModel?.addOrUpdateNoteForMonth(model.name, noteText.value)
        }
    )
}
