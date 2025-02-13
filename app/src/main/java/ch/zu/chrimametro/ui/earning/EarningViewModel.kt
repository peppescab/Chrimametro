/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.earning

import androidx.lifecycle.ViewModel
import ch.zu.chrimametro.SharedPreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class EarningsViewModel @Inject constructor(
    private val sharedPreferenceManager: SharedPreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EarningModelUiState())
    val uiState: StateFlow<EarningModelUiState> = _uiState

    private val _totalEarned = MutableStateFlow(0.0)
    val totalEarned: StateFlow<Double> = _totalEarned

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _isWorkingHours = MutableStateFlow(true)
    val isWorkingHours: StateFlow<Boolean> = _isWorkingHours

    private val _earningPerSecond = MutableStateFlow(0.0)
    val earningPerSecond: StateFlow<Double> = _earningPerSecond

    init {
        _uiState.value = sharedPreferenceManager.loadUiEarningState()
    }


    fun saveState(model: EarningModelUiState) {
        sharedPreferenceManager.storeUiEarning(model)
        _uiState.value = sharedPreferenceManager.loadUiEarningState()
    }
}

data class EarningModelUiState(
    val netMonthlySalary: String = "7142",
    val houseCost: String = "1890",
    val insuranceCost: String = "379"
)
