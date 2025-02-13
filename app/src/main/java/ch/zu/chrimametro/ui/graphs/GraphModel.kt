/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.graphs

import ch.zu.chrimametro.ui.graphs.histogram.HistogramData
import ch.zu.chrimametro.ui.graphs.pie.PieChartData

data class GraphModel(
    val currentName: String = "",
    val pieChartData: List<PieChartData> = emptyList(),
    val histogramData: List<HistogramData> = emptyList()
)
