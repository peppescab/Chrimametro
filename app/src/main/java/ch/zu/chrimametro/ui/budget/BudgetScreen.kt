/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.budget

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import ch.zu.chrimametro.R
import ch.zu.chrimametro.tools.SnackbarCustom
import ch.zu.chrimametro.tools.SwipeCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    nameMonth: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    val budgetItemListState by viewModel.myStateFlow.collectAsState(emptyList())
    val budgetItemError by viewModel.myStateFlowError.collectAsState(false)

    var showBottomSheet by remember { mutableStateOf(false) }
    var showAddBudgetItem by remember { mutableStateOf(false) }

    var toDeleteItem by remember { mutableStateOf(false) }

    var currentBudgetModel by remember { mutableStateOf<BudgetModel?>(null) }
    var currentBudgetList by remember { mutableStateOf<BudgetModel?>(null) }

    var newItemName by remember { mutableStateOf("") }
    var newItemValue by remember { mutableStateOf("") }
    var isEuro by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()


    viewModel.load(nameMonth)

    val backgroundColor = if (toDeleteItem) MaterialTheme.colorScheme.inverseOnSurface else Color.Unspecified

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {

        if (budgetItemError) {
            Toast.makeText(context, " An error occurred", Toast.LENGTH_SHORT).show()
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth() // Ensure the row takes up the full width
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, // Distribute space evenly between the buttons,
                    verticalAlignment = Alignment.CenterVertically // Center the content vertically
                ) {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Replace with your drawable
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp) // Size of the arrow icon
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f) // Allow the column to take up the remaining space
                    ) {
                        Text(
                            text = nameMonth, style = MaterialTheme.typography.titleLarge, color = MaterialTheme
                                .colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp)) // Small spacing between text and switch
                        Switch(
                            checked = toDeleteItem,
                            onCheckedChange = { toDeleteItem = it }
                        )
                    }
                    FloatingActionButton(
                        shape = FloatingActionButtonDefaults.smallShape,
                        onClick = { viewModel.sortByTaxRisk(nameMonth) },
                        modifier = Modifier.size(48.dp) // Reduce size for balance
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_sort),
                            contentDescription = "Sort",
                            modifier = Modifier.size(24.dp), // Size of the icon within the FAB
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }
            }

            items(budgetItemListState, key = { it.nameBudget }) { currentItem ->
                SwipeCard(isEnabled = toDeleteItem, onDelete = {
                    coroutineScope.launch {
                        viewModel.onDeleteItem(currentItem, nameMonth)
                        currentBudgetModel = currentItem
                        snackbarHostState.showSnackbar(
                            message = "Item deleted", actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                    }
                }) {
                    BudgetItemCard(
                        budgetModel = currentItem,
                        onAddBudgetSpecificItem = {
                            showAddBudgetItem = true
                            currentBudgetList = currentItem
                        },
                        onEditBudgetModel = {
                            showBottomSheet = true
                            currentBudgetModel = currentItem
                        }
                    )
                }
            }

        }
        FloatingActionButton(
            onClick = {
                currentBudgetModel = null
                showBottomSheet = true
            }, modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
        }

        if (showBottomSheet) {
            ModalBottomSheet(onDismissRequest = {
                showBottomSheet = false
            }) {
                BottomSheetContent(
                    budgetModel = currentBudgetModel,
                    viewModel = viewModel,
                    nameMonth = nameMonth,
                    onSave = { budgetItem ->
                        coroutineScope.launch {
                            currentBudgetModel?.let {
                                viewModel.onDeleteItem(it, nameMonth)
                            }
                            viewModel.addNewBudgetItem(budgetItem, nameMonth)
                            showBottomSheet = false
                        }
                    },
                    onCancel = {
                        showBottomSheet = false
                    })
            }
        }

        SnackbarCustom(snackbarHostState = snackbarHostState) {
            currentBudgetModel?.let {
                viewModel.addNewBudgetItem(it, nameMonth)
            }
        }
    }
}
