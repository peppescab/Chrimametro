/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.earning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.zu.chrimametro.SharedPreferenceManager
import ch.zu.chrimametro.ui.expense.MonthWithdrawModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EarningsViewModel @Inject constructor(
    private val sharedPreferenceManager: SharedPreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EarningModelUiState())
    val uiState: StateFlow<EarningModelUiState> = _uiState

    private val _monthSalaryHistory = MutableStateFlow<List<MonthWithdrawModel>>(emptyList())
    val monthSalaryHistory: StateFlow<List<MonthWithdrawModel>> = _monthSalaryHistory

    private val _totalEarned = MutableStateFlow(0.0)
    val totalEarned: StateFlow<Double> = _totalEarned

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _isWorkingHours = MutableStateFlow(true)
    val isWorkingHours: StateFlow<Boolean> = _isWorkingHours

    private val _earningPerSecond = MutableStateFlow(0.0)
    val earningPerSecond: StateFlow<Double> = _earningPerSecond

    init {
        viewModelScope.launch {
            _uiState.value = sharedPreferenceManager.loadUiEarningState()
            _monthSalaryHistory.value = sharedPreferenceManager.loadMonthWithdrawList()
        }
    }


    fun saveState(model: EarningModelUiState) {
        viewModelScope.launch {
            sharedPreferenceManager.storeUiEarning(model)
            _uiState.value = sharedPreferenceManager.loadUiEarningState()
        }
    }

    fun updateMonthFinancialDetails(
        monthName: String,
        salary: Float,
        houseCost: Float,
        insuranceCost: Float
    ) {
        viewModelScope.launch {
            sharedPreferenceManager.updateMonthFinancialDetails(
                monthName = monthName,
                salary = salary,
                houseCost = houseCost,
                insuranceCost = insuranceCost
            )
            _monthSalaryHistory.value = sharedPreferenceManager.loadMonthWithdrawList()
        }
    }

    fun refreshMonthSalaryHistory() {
        viewModelScope.launch {
            _monthSalaryHistory.value = sharedPreferenceManager.loadMonthWithdrawList()
        }
    }
}

data class EarningModelUiState(
    val netMonthlySalary: String = "7142",
    val houseCost: String = "1890",
    val insuranceCost: String = "379"
)
