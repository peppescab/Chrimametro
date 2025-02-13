/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.monthbudget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.zu.chrimametro.SharedPreferenceManager
import ch.zu.chrimametro.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MonthBudgetViewModel @Inject constructor(
    private val sharedPreferenceManager: SharedPreferenceManager
) : ViewModel() {

    private val _myStateFlow = MutableStateFlow(emptyList<MonthBudget>())
    val myStateFlow: StateFlow<List<MonthBudget>> = _myStateFlow

    fun load() {
        viewModelScope.launch {
            _myStateFlow.value = sharedPreferenceManager.loadMonthBudgetList()
        }
    }

    fun onDeleteItem(item: MonthBudget) {
        viewModelScope.launch {
            sharedPreferenceManager.removeMonthBudget(item)
            load()
        }
    }

    fun addNewMonthBudgetList() {
        viewModelScope.launch {
            val lastMonthCleaned = sharedPreferenceManager.loadMonthBudgetList().last().also { monthBudget ->
                monthBudget.nameMonth = Utils.addMonth(monthBudget.nameMonth)
                monthBudget.budgteItemList = monthBudget.budgteItemList.filterNot { it.nameBudget.contains("Cash") }
                monthBudget.calculateSum()
            }

            sharedPreferenceManager.saveMonthBudget(lastMonthCleaned)
            load()
        }
    }
}
