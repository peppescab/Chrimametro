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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewmodel @Inject constructor(
    private val sharedPreferenceManager: SharedPreferenceManager
) : ViewModel() {

    private val _myStateFlow = MutableStateFlow(emptyList<MonthWithdrawModel>())
    val myStateFlow: StateFlow<List<MonthWithdrawModel>> = _myStateFlow.asStateFlow()
    private val monthDateFormat = SimpleDateFormat("MMM yyyy", Locale.ENGLISH).apply {
        isLenient = false
    }

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
            val currentMonthName = getCurrentMonth()
            val currentDate = parseMonth(currentMonthName)

            // Remove future empty months (cleanup from previous bug)
            if (currentDate != null) {
                val futureJunk = listMonths.filter { month ->
                    val d = parseMonth(month.name)
                    d != null && d.after(currentDate) && month.expenses.isEmpty() && month.listNote.isEmpty()
                }
                futureJunk.forEach { sharedPreferenceManager.removeMonth(it) }
            }

            // Simply add current month if missing
            val refreshed = sharedPreferenceManager.loadMonthWithdrawList()
            if (refreshed.none { it.name == currentMonthName }) {
                sharedPreferenceManager.saveMonthWithdrawList(currentMonthName)
            }

            load()
        }
    }

    private fun parseMonth(value: String): Date? {
        return runCatching { monthDateFormat.parse(value) }.getOrNull()
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

    suspend fun expenseAverage(): Double {
        val listMonth = sharedPreferenceManager.loadMonthWithdrawList().toMutableList()
        if (listMonth.isEmpty()) return 0.0

        val totalSpense = listMonth.sumOf {
            it.getTotal()
        }

        return (totalSpense).div(listMonth.size)
    }
}
