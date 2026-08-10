/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.fire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.zu.chrimametro.SharedPreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FireViewModel @Inject constructor(
    private val sharedPreferenceManager: SharedPreferenceManager
) : ViewModel() {
    
    private val _simulationState = MutableStateFlow(FireSimulationState())
    val simulationState: StateFlow<FireSimulationState> = _simulationState.asStateFlow()
    
    private val engine = FinancialSimulationEngine()
    private var simulationCounter = 0
    
    init {
        loadInputs()
    }
    
    private fun loadInputs() {
        viewModelScope.launch {
            val inputs = FireInputs()
            _simulationState.value = _simulationState.value.copy(inputs = inputs)
            runSimulation()
        }
    }
    
    fun updateInput(update: (FireInputs) -> FireInputs) {
        val newInputs = update(_simulationState.value.inputs)
        _simulationState.value = _simulationState.value.copy(inputs = newInputs)
        runSimulation()
    }
    
    fun updateCurrentAge(age: Int) {
        if (age <= 0) return
        updateInput { it.copy(currentAge = age) }
    }
    
    fun updateTargetFireAge(age: Int) {
        if (age <= 0) return
        updateInput { it.copy(targetFireAge = age) }
    }

    fun updateLifeExpectancy(age: Int) {
        if (age <= 0) return
        updateInput { it.copy(lifeExpectancy = age) }
    }

    fun updateReturnToItalyYear(year: Int) {
        if (year <= 0) return
        updateInput { it.copy(returnToItalyYear = year) }
    }
    
    fun updateSwissYearlySavings(amount: Double) {
        updateInput { it.copy(swissYearlySavings = amount) }
    }
    
    fun updateItalianYearlySavings(amount: Double) {
        updateInput { it.copy(italianYearlySavings = amount) }
    }
    
    fun updateMonthlySpendings(amount: Double) {
        updateInput { it.copy(monthlySpendings = amount) }
    }
    
    fun updateExpectedInflation(rate: Double) {
        updateInput { it.copy(expectedInflation = rate) }
    }
    
    fun updateExpectedEtfReturn(rate: Double) {
        updateInput { it.copy(expectedEtfReturn = rate) }
    }
    
    fun updateExpectedBondReturn(rate: Double) {
        updateInput { it.copy(expectedBondReturn = rate) }
    }
    
    fun updateExpectedCryptoReturn(rate: Double) {
        updateInput { it.copy(expectedCryptoReturn = rate) }
    }

    fun updateExpectedGoldReturn(rate: Double) {
        updateInput { it.copy(expectedGoldReturn = rate) }
    }
    
    fun updateExpectedPension(amount: Double) {
        updateInput { it.copy(expectedPension = amount) }
    }
    
    fun updateThirdPillarStrategy(strategy: ThirdPillarStrategy) {
        updateInput { it.copy(thirdPillarStrategy = strategy) }
    }

    fun updateThirdPillarRedemptionTaxRate(rate: Double) {
        updateInput { it.copy(thirdPillarRedemptionTaxRate = rate.coerceAtLeast(0.0)) }
    }

    fun updateTargetAllocation(assetName: String, percentage: Double) {
        updateInput { inputs ->
            val updated = inputs.targetAllocations
                .filterNot { it.assetName == assetName } + AllocationTarget(assetName, percentage.coerceAtLeast(0.0))
            inputs.copy(targetAllocations = updated)
        }
    }

    fun updateWithdrawalStrategy(strategy: WithdrawalStrategy) {
        updateInput { it.copy(withdrawalStrategy = strategy) }
    }

    fun updateSimulationMode(mode: SimulationMode) {
        updateInput { it.copy(simulationMode = mode) }
    }

    fun updateAssets(assets: List<AssetType>) {
        updateInput { it.copy(assets = assets) }
    }
    
    fun addAsset(asset: AssetType) {
        updateInput { it.copy(assets = it.assets + asset) }
    }
    
    fun removeAsset(index: Int) {
        updateInput { 
            it.copy(assets = it.assets.filterIndexed { i, _ -> i != index })
        }
    }

    fun removeAssetByName(name: String) {
        updateInput {
            it.copy(assets = it.assets.filterNot { asset -> asset.name == name })
        }
    }
    
    fun updateAsset(index: Int, asset: AssetType) {
        updateInput {
            it.copy(assets = it.assets.mapIndexed { i, a ->
                if (i == index) asset else a
            })
        }
    }

    fun updateAssetValue(assetName: String, newValue: Double) {
        updateInput {
            it.copy(assets = it.assets.map { asset ->
                if (asset.name == assetName) {
                    asset.copy(currentValue = newValue.coerceAtLeast(0.0))
                } else {
                    asset
                }
            })
        }
    }
    
    private fun runSimulation() {
        val currentCounter = ++simulationCounter
        viewModelScope.launch {
            try {
                _simulationState.value = _simulationState.value.copy(isLoading = true, error = null)
                val outputs = engine.simulate(_simulationState.value.inputs)
                // Only update if this is still the latest simulation
                if (currentCounter == simulationCounter) {
                    _simulationState.value = _simulationState.value.copy(
                        outputs = outputs,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                if (currentCounter == simulationCounter) {
                    _simulationState.value = _simulationState.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    fun saveInputs() {
        viewModelScope.launch {
            // TODO: Save to SharedPreferences when persistence is implemented
        }
    }
}
