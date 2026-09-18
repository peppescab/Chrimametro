/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.zu.chrimametro.Utils.getCurrentMonth
import ch.zu.chrimametro.ui.theme.ChrimametroTheme
import kotlinx.coroutines.flow.collect

@Composable
fun ExpensesScreen(viewModel: MainViewmodel) {
    val myState by viewModel.myStateFlow.collectAsState(emptyList())
    val latestState by rememberUpdatedState(myState)
    val listState = rememberLazyListState()
    val currentMonthName = getCurrentMonth()
    val currentMonth = myState.firstOrNull { it.name == currentMonthName }
    val otherMonths = myState.filterNot { it.name == currentMonthName }

    LaunchedEffect(viewModel) {
        viewModel.scrollToMonth.collect { monthName ->
            val targetIndex = latestState.indexOfFirst { it.name == monthName }
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 92.dp)
        ) {
            currentMonth?.let { month ->
                item(key = month.name) {
                    MonthlyCard(
                        model = month,
                        viewModel = viewModel,
                        initiallyExpanded = true,
                        hero = true
                    )
                }
            }

            if (otherMonths.isNotEmpty()) {
                item {
                    androidx.compose.material3.Text(
                        text = "Other months",
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
            }

            items(otherMonths, key = { it.name }) { month ->
                MonthlyCard(model = month, viewModel = viewModel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MonthlyCardPreview() {
    ChrimametroTheme {
        MonthlyCard(
            MonthWithdrawModel("Gen 23", mutableListOf(999.9f, 900.9f), salary = 7140f, fixedCosts = 2690f),
            null
        )
    }
}