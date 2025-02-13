/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.monthbudget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ch.zu.chrimametro.tools.SimpleAlertDialog

@Composable
fun MonthBudgetScreen(
    viewModel: MonthBudgetViewModel, navController: NavController
) {
    val budgetItemListState by viewModel.myStateFlow.collectAsState(emptyList())

    var deleteMonth by remember { mutableStateOf(false) }
    var currentMonthBudgetModel by remember { mutableStateOf<MonthBudget?>(null) }
    viewModel.load()
    Box(modifier = Modifier.fillMaxSize()) {

        LazyColumn(modifier = Modifier.fillMaxSize()) {
/*
            item {
                FloatingActionButton(
                    onClick = { },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_sort_risk),
                        contentDescription = "Sort",
                        modifier = Modifier.size(24.dp),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }*/
//TODO
            /*
            items(budgetItemListState.reversed(), key = { it.nameMonth }) { currentItem ->
                MonthBudgetCard(currentItem,
                    onClickItem = { navController.navigate("${Screen.Budget.route}/${currentItem.nameMonth}") },
                    onDoubleClickItem = {
                        deleteMonth = true
                        currentMonthBudgetModel = currentItem
                    })
            }*/

        }
        FloatingActionButton(
            onClick = {
                currentMonthBudgetModel = null
                viewModel.addNewMonthBudgetList()
            }, modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
        }

        SimpleAlertDialog(
            isVisible = deleteMonth,
            onDismiss = { deleteMonth = false },
            onConfirm = {
                currentMonthBudgetModel?.let {
                    viewModel.onDeleteItem(it)
                }
            }, title = "Do you want to remove " + currentMonthBudgetModel?.nameMonth
        )

    }
}

