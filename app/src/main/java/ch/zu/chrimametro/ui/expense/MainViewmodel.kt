/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.zu.chrimametro.SharedPreferenceManager
import ch.zu.chrimametro.Utils
import ch.zu.chrimametro.Utils.getCurrentMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewmodel @Inject constructor(
    private val sharedPreferenceManager: SharedPreferenceManager
) : ViewModel() {

    private val _myStateFlow = MutableStateFlow(emptyList<MonthWithdrawModel>())
    val myStateFlow: StateFlow<List<MonthWithdrawModel>> = _myStateFlow.asStateFlow()

    init {
        addNewMonthIfPossible()
    }

    fun load() {
        viewModelScope.launch {
            _myStateFlow.value = sharedPreferenceManager.loadMonthWithdrawList()
        }
    }

    fun storeInput(nameMonth: String, inputVal: Float) {
        viewModelScope.launch {
            sharedPreferenceManager.saveMonthWithdrawList(nameMonth, valueToAdd = inputVal)
            load()
        }
    }

    fun deleteEntry(nameMonth: String, inputVal: Float) {
        viewModelScope.launch {
            sharedPreferenceManager.removeMonthWithdrawList(nameMonth, amountToRemove = inputVal)
            load()
        }
    }

    fun deleteNote(nameMonth: String, inputVal: String) {
        viewModelScope.launch {
            sharedPreferenceManager.removeMonthWithdrawList(nameMonth, noteToRemove = inputVal)
            load()
        }
    }

    private fun addNewMonthIfPossible() {
        viewModelScope.launch {
            val listMonths = sharedPreferenceManager.loadMonthWithdrawList()
            if (listMonths.find { it.name == getCurrentMonth() } == null) {
                sharedPreferenceManager.saveMonthWithdrawList(getCurrentMonth())
            }
            load()
        }
    }

    fun addNextMonth() {
        viewModelScope.launch {
            val listMonths = sharedPreferenceManager.loadMonthWithdrawList().first()
            sharedPreferenceManager.saveMonthWithdrawList(Utils.addMonth(listMonths.name))
            load()
        }
    }

    private fun removeUpTo() {
        viewModelScope.launch {
            val listMonths = sharedPreferenceManager.loadMonthWithdrawList()
            val filteredList = listMonths.filterNot {
                it.name.contains("" + 2024)
                    || it.name.contains("" + 2023)
            }
            filteredList.forEach {
                sharedPreferenceManager.removeMonth(it)
            }
        }
        load()
    }

    fun deleteMonth(month: MonthWithdrawModel) {
        viewModelScope.launch {
            sharedPreferenceManager.removeMonth(month)
            load()
        }
    }

    fun addOrUpdateNoteForMonth(name: String, value: String) {
        viewModelScope.launch {
            sharedPreferenceManager.saveMonthWithdrawList(name, noteToAdd = value)
            load()
        }
    }

    fun expenseAverage(): Double {
        val listMonth = sharedPreferenceManager.loadMonthWithdrawList().toMutableList()

        val totalSpense = listMonth.sumOf {
            it.getTotal()
        }

        return (totalSpense).div(listMonth.size)
    }
}
