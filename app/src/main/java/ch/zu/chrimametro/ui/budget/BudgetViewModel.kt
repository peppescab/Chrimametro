/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.zu.chrimametro.SharedPreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val sharedPreferenceManager: SharedPreferenceManager
) : ViewModel() {

    private val _myStateFlow = MutableStateFlow(emptyList<BudgetModel>())
    val myStateFlow: StateFlow<List<BudgetModel>> = _myStateFlow

    private val _myStateFlowError = MutableStateFlow(false)
    val myStateFlowError: StateFlow<Boolean> = _myStateFlowError

    //var currentMonth: String = ""

    fun load(nameMonth: String) {
        viewModelScope.launch {
            // currentMonth?.let {
            _myStateFlow.value = sharedPreferenceManager.loadBudgetItemList(nameMonth)
            //}
        }
    }

    fun onDeleteItem(item: BudgetModel, nameMonth: String) {
        viewModelScope.launch {
            sharedPreferenceManager.removeBudgetItemList(item, nameMonth)
            _myStateFlow.value = sharedPreferenceManager.loadBudgetItemList(nameMonth)
        }
    }

    fun addNewBudgetItem(item: BudgetModel, nameMonth: String) {
        viewModelScope.launch {
            val isSaved = sharedPreferenceManager.saveBudgetItemList(item, nameMonth)
            if (!isSaved) _myStateFlowError.value = true
            load(nameMonth)
        }
    }

    fun sortByTaxRisk(nameMonth: String) {
        viewModelScope.launch {
            sharedPreferenceManager.sortBudgetItemList(nameMonth)
            load(nameMonth)
        }
    }

    fun addSpecificItemToBudget(item: BudgetModel, newItem: BudgetSpecificItem,nameMonth:String) {
        viewModelScope.launch {
            sharedPreferenceManager.saveSpecificItem(item, newItem, nameMonth)
            load(nameMonth)
        }
    }

    fun removeSpecificItemToBudget(item: BudgetModel, newItem: BudgetSpecificItem,nameMonth:String) {
        viewModelScope.launch {
            sharedPreferenceManager.removeSpecificItem(item, newItem, nameMonth)
            load(nameMonth)
        }
    }
}
