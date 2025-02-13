/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.budget

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetContent(
    budgetModel: BudgetModel? = null,
    onSave: (BudgetModel) -> Unit,
    viewModel: BudgetViewModel,
    nameMonth: String,
    onCancel: () -> Unit
) {
    var nameItem by remember { mutableStateOf(budgetModel?.nameBudget.orEmpty()) }
    var moneyAdd by remember { mutableStateOf(budgetModel?.moneyAdded?.toString().orEmpty()) }
    var toBeTaxed by remember { mutableStateOf(budgetModel?.toBeTaxed ?: false) }
    var actualValue by remember { mutableStateOf(budgetModel?.actualValue?.toString().orEmpty()) }
    var levelOfRisk by remember { mutableFloatStateOf(budgetModel?.levelOfRisk ?: 0.0f) }
    var expanded by remember { mutableStateOf(false) }
    var currentValueShowed by remember { mutableStateOf(false) }
    var selectedBudgetType by remember { mutableStateOf(budgetModel?.type ?: BudgetType.Various) }

    var currency by remember { mutableStateOf(budgetModel?.currencyChf ?: false) }

    var showAddBudgetItem by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemValue by remember { mutableStateOf("") }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = "Add Budget Specific Item", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(), // Ensure the row fills the available width
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Chf")
            Switch(
                checked = currency,
                onCheckedChange = { currency = it }
            )
            OutlinedTextField(
                value = nameItem,
                onValueChange = { nameItem = it },
                label = { Text("Item Name") },
                modifier = Modifier
                    .weight(1f) // Give equal space to both fields
                    .fillMaxWidth()
            )
            OutlinedTextField(
                value = moneyAdd,
                onValueChange = { moneyAdd = it },
                label = { Text("Money Added") },
                modifier = Modifier
                    .weight(1f) // Give equal space to both fields
                    .fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (currentValueShowed || actualValue.isNotEmpty()) {
            OutlinedTextField(
                value = actualValue,
                onValueChange = { actualValue = it },
                label = { Text("Actual Value") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = "Copy",
                        modifier = Modifier.clickable {
                            actualValue = moneyAdd
                        }
                    )
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Show Actual Value")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = currentValueShowed,
                    onCheckedChange = { currentValueShowed = it }
                )
            }
        }

        Text(text = "Level of Risk")
        Row(
            modifier = Modifier.fillMaxWidth(), // Ensure the row fills the available width
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = String.format("%.2f", levelOfRisk)) // Show the current value
            Slider(
                value = levelOfRisk,
                onValueChange = { levelOfRisk = it },
                valueRange = 0f..1f,
                steps = 5,
                modifier = Modifier.wrapContentSize()
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(), // Ensure the row fills the available width
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = "Be Taxed", modifier = Modifier.wrapContentSize()
            )
            Switch(
                checked = toBeTaxed,
                onCheckedChange = { toBeTaxed = it },
                modifier = Modifier.wrapContentSize()
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Dropdown Menu for BudgetType
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedBudgetType.name,
                    onValueChange = { },
                    label = { Text("Select Budget Type") },
                    trailingIcon = {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Dropdown"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    BudgetType.entries.forEach { budgetType ->
                        DropdownMenuItem(
                            text = { Text(budgetType.name) },
                            onClick = {
                                selectedBudgetType = budgetType
                                expanded = false
                            },
                            leadingIcon = {
                                Image(
                                    painter = painterResource(id = budgetType.imgResource),
                                    contentDescription = budgetType.name,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        budgetModel?.specificItemList?.forEach {
            Row {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "delete",
                    modifier = Modifier.clickable {
                        viewModel.removeSpecificItemToBudget(budgetModel, it, nameMonth)
                    }
                )
                Text(it.nameItem + " :: " + it.moneyAdd)
            }
        }

        Row {
            Text("New Specific Item ? ")
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "delete",
                modifier = Modifier.clickable {
                    showAddBudgetItem = true
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                val updatedBudgetModel = budgetModel?.copy(
                    nameBudget = nameItem,
                    moneyAdded = moneyAdd.toIntOrNull() ?: 0,
                    actualValue = actualValue.toIntOrNull(),
                    toBeTaxed = toBeTaxed,
                    levelOfRisk = levelOfRisk,
                    type = selectedBudgetType,
                    currencyChf = currency
                ) ?: BudgetModel(
                    nameBudget = nameItem,
                    moneyAdded = moneyAdd.toIntOrNull() ?: 0,
                    actualValue = actualValue.toIntOrNull(),
                    toBeTaxed = toBeTaxed,
                    levelOfRisk = levelOfRisk,
                    type = selectedBudgetType,
                    currencyChf = currency
                )
                onSave(updatedBudgetModel)
            }) {
                Text("Save")
            }

            Spacer(modifier = Modifier.height(16.dp))

        }

        if (showAddBudgetItem) {
            AlertDialog(onDismissRequest = { showAddBudgetItem = false }, confirmButton = {
                Button(onClick = {
                    budgetModel?.let { item ->
                        val newItem = BudgetSpecificItem(nameItem = newItemName, moneyAdd = newItemValue.toInt())
                        viewModel.addSpecificItemToBudget(item, newItem, nameMonth)
                        showAddBudgetItem = false
                        newItemName = ""
                        newItemValue = ""
                    }
                }) {
                    Text("Add")
                }
            }, dismissButton = {
                Button(onClick = { showAddBudgetItem = false }) {
                    Text("Cancel")
                }
            }, title = { Text("Add Specific Item") }, text = {
                Column {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    OutlinedTextField(
                        value = newItemValue,
                        onValueChange = { newItemValue = it },
                        label = { Text("Value") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
            })
        }
    }
}
