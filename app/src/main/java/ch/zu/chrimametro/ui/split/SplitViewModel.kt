/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.split

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SplitViewModel @Inject constructor(
) : ViewModel() {

    private val defaultSplitModel = listOf(
        SplitModel(
            name = "Dyanema",
            totalSum = 100000,
            listInvestors = listOf(
                Investor("Io", 33),
                Investor("Gio", 40),
                Investor("Silvia", 12),
                Investor("Konstantinos", 5),
                Investor("Luisa", 10)
            ),
            incomePercentage = 16
        ),
        SplitModel(
            name = "Tommy",
            totalSum = 75000,
            listInvestors = listOf(
                Investor("Io", 35),
                Investor("Gio", 35),
                Investor("Silvia", 5),
                Investor("Luisa", 5)
            ),
            incomePercentage = 10
        ),
    )

    private val _myStateFlow = MutableStateFlow<List<SplitModel>>(emptyList())
    val myStateFlow: StateFlow<List<SplitModel>> = _myStateFlow.asStateFlow()

    init {
        getSplitModel()
    }

    private fun getSplitModel() {
        _myStateFlow.value = defaultSplitModel
    }
}
