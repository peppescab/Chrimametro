/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.graphs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.zu.chrimametro.SharedPreferenceManager
import ch.zu.chrimametro.ui.getBackGroundColor
import ch.zu.chrimametro.ui.graphs.histogram.HistogramData
import ch.zu.chrimametro.ui.graphs.pie.PieChartData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val sharedPreferenceManager: SharedPreferenceManager
) : ViewModel() {

    private val _myStateFlow = MutableStateFlow(GraphModel())
    val myStateFlow: StateFlow<GraphModel> = _myStateFlow

    var currentGraph = GraphModel()

    init {
        viewModelScope.launch {
            val listPieChart = mutableListOf<PieChartData>()
            val listHistogramData = mutableListOf<HistogramData>()
            val listMonthBudget = sharedPreferenceManager.loadMonthBudgetList()

            val monthForPieChart = listMonthBudget.last()

            monthForPieChart.budgteItemList.forEach {
                listPieChart.add(
                    PieChartData(
                        (it.moneyAdded.toFloat() / listMonthBudget.first().totalAmount.toFloat()),
                        getBackGroundColor(it.levelOfRisk)
                    )
                )
            }

            listMonthBudget.forEach {
                listHistogramData.add(
                    HistogramData(
                        it.nameMonth,
                        it.currentAmount
                    )
                )
            }
            currentGraph = GraphModel(monthForPieChart.nameMonth, listPieChart, listHistogramData)
            _myStateFlow.value = currentGraph
        }
    }

    fun monthClicked(nameOfMonth: String) {
        viewModelScope.launch {
            val listPieChart = mutableListOf<PieChartData>()
            val listMonthBudget = sharedPreferenceManager.loadMonthBudgetList().find {
                it.nameMonth == nameOfMonth
            }
            listMonthBudget?.budgteItemList?.forEach {
                listPieChart.add(
                    PieChartData(
                        (it.moneyAdded.toFloat() / listMonthBudget.totalAmount.toFloat()),
                        getBackGroundColor(it.levelOfRisk)
                    )
                )
            }
            _myStateFlow.value = currentGraph.copy(currentName = nameOfMonth, pieChartData = listPieChart)
        }
    }
}
