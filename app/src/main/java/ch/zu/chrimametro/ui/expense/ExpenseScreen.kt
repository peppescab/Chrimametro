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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.zu.chrimametro.R
import ch.zu.chrimametro.ui.theme.ChrimametroTheme

@Composable
fun ExpensesScreen(viewModel: MainViewmodel) {
    val myState by viewModel.myStateFlow.collectAsState(emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 92.dp)
        ) {
            items(myState, key = { it.name }) { month ->
                MonthlyCard(
                    model = month,
                    viewModel = viewModel
                )
            }
        }

        FloatingActionButton(
            onClick = { viewModel.addNextMonth() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_add_month)
            )
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