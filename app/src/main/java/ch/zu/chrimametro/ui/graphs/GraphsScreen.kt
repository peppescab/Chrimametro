/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.graphs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.zu.chrimametro.ui.graphs.histogram.HistogramChart
import ch.zu.chrimametro.ui.graphs.pie.SimplePieChart

@Composable
fun GraphsScreen(
    viewModel: GraphViewModel
) {

    val budgetItemListState by viewModel.myStateFlow.collectAsState()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = budgetItemListState.currentName, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.size(16.dp))
        SimplePieChart(pieChartData = budgetItemListState.pieChartData, modifier = Modifier.size(300.dp))
        Spacer(modifier = Modifier.size(16.dp))
        HistogramChart(histogramData = budgetItemListState.histogramData) { nameMonth ->
            viewModel.monthClicked(nameMonth)
        }
    }
}
