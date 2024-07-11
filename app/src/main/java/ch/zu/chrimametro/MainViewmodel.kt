/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewmodel @Inject constructor(
    private val sharedPreferenceManager: SharedPreferenceManager
) : ViewModel() {

    private val _myStateFlow = MutableStateFlow(emptyList<MonthWithdrawModel>())
    val myStateFlow: SharedFlow<List<MonthWithdrawModel>> = _myStateFlow.asSharedFlow()

    fun load() {
        viewModelScope.launch {
            _myStateFlow.value = sharedPreferenceManager.loadMonthWithdrawList()
        }
    }

    fun storeInput(nameMonth: String, inputVal: Float) {
        viewModelScope.launch {
            sharedPreferenceManager.saveMonthWithdrawList(nameMonth, inputVal)
            load()
        }
    }

    fun deleteEntry(nameMonth: String, inputVal: Float) {
        viewModelScope.launch {
            sharedPreferenceManager.removeMonthWithdrawList(nameMonth, inputVal)
            load()
        }
    }

    fun addNewMonth() {
        viewModelScope.launch {
            val listMonths = sharedPreferenceManager.loadMonthWithdrawList()
            val newMonth = addMonth(listMonths.first().name)
            sharedPreferenceManager.saveMonthWithdrawList(newMonth)
            load()
        }
    }

    private fun addMonth(monthS: String): String {
        // Define the date format
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)

        // Parse the input string into a Date object
        val date = dateFormat.parse(monthS)

        // Convert the Date object to a Calendar object for manipulation
        val calendar = Calendar.getInstance()
        calendar.time = date

        // Increment the date by one month
        calendar.add(Calendar.MONTH, 1)

        // Check if the incremented month is January (month index 0)
        if (calendar[Calendar.MONTH] == Calendar.JANUARY) {
            // Adjust the year if the incremented month is January
            calendar.add(Calendar.YEAR, 1)
        }

        return dateFormat.format(calendar.time)
    }
}
