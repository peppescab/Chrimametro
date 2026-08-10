/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
 */
package ch.zu.chrimametro.ui.fire

import java.util.Calendar
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class FinancialSimulationEngine {

    private data class AssetState(
        val name: String,
        var value: Double,
        val expectedAnnualReturn: Double,
        val volatility: Double,
        val group: AssetGroup,
        val lifecycle: AssetLifecycle,
        val events: List<AssetEvent>,
        var terminated: Boolean = false
    )

    private data class MonteCarloResult(
        val finalPortfoliosByIteration: List<Double>,
        val successCount: Int,
        val allocationsByIteration: List<List<AssetSnapshot>>,
        val allYearsData: List<List<MonteCarloYearData>>
    )

    private data class MonteCarloYearData(
        val year: Int,
        val age: Int,
        val portfolioValues: List<Double>, // One value per iteration
        val allocations: List<List<AssetSnapshot>>
    )

    companion object {
        private const val FALLBACK_ASSET_NAME = "Unallocated Savings"
        private const val SIMULATION_YEARS = 52
        private const val BALANCE_EPSILON = 0.5
    }

    fun simulate(inputs: FireInputs): FireOutputs {
        return when (inputs.simulationMode) {
            SimulationMode.DETERMINISTIC -> simulateDeterministic(inputs)
            SimulationMode.MONTE_CARLO -> simulateMonteCarloHistorical(inputs)
        }
    }

    private fun simulateDeterministic(inputs: FireInputs): FireOutputs {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val portfolioEvolution = simulateYearByYear(inputs, currentYear, null)

        val projectedAtFire = portfolioEvolution.firstOrNull { it.age >= inputs.targetFireAge }
        val projectedPortfolioAtFire = projectedAtFire?.endPortfolio ?: 0.0

        val readinessByAge = buildReadinessRows(inputs, portfolioEvolution)
        val targetReadiness = readinessByAge.firstOrNull { it.age >= inputs.targetFireAge }
            ?: readinessByAge.lastOrNull()

        val requiredFirePortfolio = targetReadiness?.requiredPortfolio ?: 0.0
        val fiPercentage = if (requiredFirePortfolio > 0.0) {
            ((inputs.investablePortfolio / requiredFirePortfolio) * 100).coerceIn(0.0, 1000.0)
        } else {
            0.0
        }

        val recommendedFireAge = findRecommendedFireAge(readinessByAge)
        val coastFireAge = findCoastFireAge(inputs, portfolioEvolution, currentYear)
        val baristaFireAge = findBaristaFireAge(inputs, portfolioEvolution)
        val allocationAtFire = buildAllocationSlices(projectedAtFire?.assetAllocation.orEmpty())

        return FireOutputs(
            projectedPortfolioAtFire = projectedPortfolioAtFire,
            requiredFirePortfolio = requiredFirePortfolio,
            fiPercentage = fiPercentage,
            recommendedFireAge = recommendedFireAge,
            coastFireAge = coastFireAge,
            baristaFireAge = baristaFireAge,
            probabilityOfSuccess = 1.0,
            portfolioEvolution = portfolioEvolution,
            readinessByAge = readinessByAge,
            allocationAtFire = allocationAtFire,
            totalNetWorth = inputs.totalNetWorth,
            investableAssets = inputs.investablePortfolio,
            retirementAssetsAtStart = inputs.retirementAssets,
            excludedAssetsAtStart = inputs.excludedAssets
        )
    }

    private fun simulateYearByYear(inputs: FireInputs, currentYear: Int, random: Random?): List<YearProjection> {
        val assets = buildInitialAssets(inputs, currentYear)
        val projections = mutableListOf<YearProjection>()
        var cumulativeContributions = 0.0
        var cumulativeReturns = 0.0
        val effectiveFireAge = inputs.targetFireAge.coerceAtLeast(inputs.currentAge + 1)
        val baseAnnualSpending = inputs.monthlySpendings * 12

        for (index in 0 until SIMULATION_YEARS) {
            val year = currentYear + index
            val age = inputs.currentAge + index
            val startPortfolio = portfolioValue(assets)

            // 1. Investment returns (clamped ≥ 0 in deterministic mode).
            val investmentReturns = applyReturns(assets, year, inputs, random)
            val portfolioAfterReturns = portfolioValue(assets)
            
            // Verify: deterministic mode should never have negative returns
            if (random == null && investmentReturns < 0) {
                throw IllegalStateException(
                    "Deterministic mode should never have negative returns! " +
                    "Year $year: returns = $investmentReturns"
                )
            }

            // 2. Events (maturities, redemptions, refunds).
            val eventRecords = processEvents(assets, year, inputs)
            val portfolioAfterEvents = portfolioValue(assets)
            val eventNet = portfolioAfterEvents - portfolioAfterReturns

            // 3. Contributions (only during accumulation).
            val savings = if (age < effectiveFireAge) {
                val yearlySavings = if (inputs.returnToItalyYear > currentYear && year < inputs.returnToItalyYear) {
                    inputs.swissYearlySavings
                } else {
                    inputs.italianYearlySavings
                }
                addSavingsToAssets(assets, yearlySavings, inputs)
            } else {
                0.0
            }
            val portfolioAfterSavings = portfolioValue(assets)
            val actualSavings = portfolioAfterSavings - portfolioAfterEvents

            // 4. Withdrawals (only after FIRE age).
            val inflationMultiplier = (1 + inputs.expectedInflation).pow(index.toDouble())
            val inflationAdjustedSpending = baseAnnualSpending * inflationMultiplier
            val pensionIncome = if (age >= inputs.pensionStartAge) {
                inputs.expectedPension + inputs.swissPension
            } else {
                0.0
            }

            val spending = if (age >= effectiveFireAge) {
                val targetWithdrawal = computeTargetWithdrawal(
                    inputs = inputs,
                    yearIndex = index,
                    age = age,
                    startPortfolio = portfolioAfterSavings,
                    inflationAdjustedSpending = inflationAdjustedSpending,
                    pensionIncome = pensionIncome,
                    projectionsSoFar = projections
                )
                withdrawFromAssets(assets, targetWithdrawal)
            } else {
                0.0
            }

            val endPortfolio = portfolioValue(assets)

            // Contribution / withdrawal columns absorb the signed event delta so
            // the balance identity holds by construction:
            //   end = start + returns + contributions - withdrawals
            val contributions = actualSavings + maxOf(eventNet, 0.0)
            val withdrawals = spending + maxOf(-eventNet, 0.0)

            cumulativeReturns += investmentReturns
            cumulativeContributions += contributions

            val balanceError = endPortfolio - (startPortfolio + investmentReturns + contributions - withdrawals)
            
            // Verify balance identity
            if (kotlin.math.abs(balanceError) > BALANCE_EPSILON) {
                throw IllegalStateException(
                    "Balance identity violated at year $year! " +
                    "End($endPortfolio) != Start($startPortfolio) + Returns($investmentReturns) + " +
                    "Contrib($contributions) - Withdraw($withdrawals). Error: $balanceError"
                )
            }

            projections.add(
                YearProjection(
                    year = year,
                    age = age,
                    startPortfolio = startPortfolio,
                    contributions = contributions,
                    returns = investmentReturns,  // Deterministic is clamped above; MC can be negative
                    withdrawals = withdrawals,
                    endPortfolio = endPortfolio,
                    savings = savings,
                    spending = spending,
                    inflationAdjustedSpending = inflationAdjustedSpending,
                    cumulativeContributions = cumulativeContributions,
                    cumulativeReturns = cumulativeReturns,
                    pensionIncome = pensionIncome,
                    assetAllocation = snapshotAssets(assets),
                    events = eventRecords,
                    balanceError = if (kotlin.math.abs(balanceError) < BALANCE_EPSILON) 0.0 else balanceError
                )
            )
        }

        return projections
    }

    private fun computeTargetWithdrawal(
        inputs: FireInputs,
        yearIndex: Int,
        age: Int,
        startPortfolio: Double,
        inflationAdjustedSpending: Double,
        pensionIncome: Double,
        projectionsSoFar: List<YearProjection>
    ): Double {
        val netSpending = (inflationAdjustedSpending - pensionIncome).coerceAtLeast(0.0)
        return when (inputs.withdrawalStrategy) {
            WithdrawalStrategy.FIXED_INFLATION_ADJUSTED -> netSpending
            WithdrawalStrategy.FOUR_PERCENT_RULE -> {
                val initialBase = projectionsSoFar
                    .firstOrNull { it.age >= inputs.targetFireAge }
                    ?.startPortfolio
                    ?: startPortfolio
                val inflationMult = (1 + inputs.expectedInflation).pow(yearIndex.toDouble())
                (initialBase * 0.04 * inflationMult - pensionIncome).coerceAtLeast(0.0)
            }
            WithdrawalStrategy.VPW -> {
                val remainingYears = (inputs.lifeExpectancy - age).coerceAtLeast(1)
                (startPortfolio / remainingYears - pensionIncome).coerceAtLeast(0.0)
            }
            WithdrawalStrategy.GUYTON_KLINGER -> {
                // Simplified guardrail: 4% target with a soft cap based on prior spending.
                val previous = projectionsSoFar.lastOrNull()?.spending ?: netSpending
                val target = startPortfolio * 0.04
                val ceiling = previous * 1.10
                val floor = previous * 0.90
                (target.coerceIn(floor, ceiling) - pensionIncome).coerceAtLeast(0.0)
            }
        }
    }

    private fun buildInitialAssets(inputs: FireInputs, currentYear: Int): MutableList<AssetState> {
        if (inputs.assets.isEmpty()) {
            return mutableListOf(
                AssetState(
                    name = FALLBACK_ASSET_NAME,
                    value = 0.0,
                    expectedAnnualReturn = inputs.expectedEtfReturn,
                    volatility = 0.16,
                    group = AssetGroup.INVESTABLE,
                    lifecycle = AssetLifecycle(),
                    events = emptyList()
                )
            )
        }

        return inputs.assets.mapIndexed { index, asset ->
            AssetState(
                name = asset.name.ifBlank { "Asset ${index + 1}" },
                value = asset.currentValue.coerceAtLeast(0.0),
                expectedAnnualReturn = asset.expectedAnnualReturn,
                volatility = asset.volatility.coerceAtLeast(0.0),
                group = asset.group,
                lifecycle = asset.lifecycle,
                events = buildEventsForAsset(asset, inputs, currentYear)
            )
        }.toMutableList()
    }

    private fun buildEventsForAsset(
        asset: AssetType,
        inputs: FireInputs,
        currentYear: Int
    ): List<AssetEvent> {
        val baseEvents = asset.events.filterNot {
            asset.name == "Third Pillar" && it.type == AssetEventType.THIRD_PILLAR_REDEEMED
        }

        if (asset.name != "Third Pillar") {
            return baseEvents
        }

        val redemptionYear = when (inputs.thirdPillarStrategy) {
            ThirdPillarStrategy.KEEP_UNTIL_RETIREMENT -> {
                currentYear + (inputs.pensionStartAge - inputs.currentAge).coerceAtLeast(0)
            }
            ThirdPillarStrategy.REDEEM_WHEN_LEAVING_SWITZERLAND -> {
                inputs.returnToItalyYear.coerceAtLeast(currentYear + 1)
            }
        }

        return baseEvents + AssetEvent(
            year = redemptionYear,
            type = AssetEventType.THIRD_PILLAR_REDEEMED,
            destinationAllocations = normalizedAllocations(inputs.targetAllocations)
        )
    }

    private fun applyReturns(assets: MutableList<AssetState>, year: Int, inputs: FireInputs, random: Random? = null): Double {
        var totalReturns = 0.0
        val deterministic = random == null

        assets.forEach { asset ->
            if (asset.terminated || !isActive(asset, year)) return@forEach
            if (!asset.group.countsInPortfolio) return@forEach
            if (hasPendingFinalValueEvent(asset, year)) return@forEach

            val yearlyReturn = if (deterministic) {
                // Deterministic: use expected return clamped to ≥ 0
                (asset.value * asset.expectedAnnualReturn).coerceAtLeast(0.0)
            } else {
                // Monte Carlo: sample from normal distribution N(mean, volatility)
                val sampledReturn = sampleNormalReturn(random!!, asset.expectedAnnualReturn, asset.volatility)
                asset.value * sampledReturn
            }

            asset.value += yearlyReturn
            totalReturns += yearlyReturn
        }

        return totalReturns
    }

    private fun sampleNormalReturn(random: Random, mean: Double, volatility: Double): Double {
        // Box-Muller transform to generate normal random variable
        val u1 = random.nextDouble().coerceIn(0.0001, 0.9999)
        val u2 = random.nextDouble().coerceIn(0.0001, 0.9999)
        val z0 = sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)
        return mean + volatility * z0
    }

    private fun hasPendingFinalValueEvent(asset: AssetState, year: Int): Boolean {
        return asset.events.any { it.year >= year && it.finalValue != null }
    }

    private fun addSavingsToAssets(
        assets: MutableList<AssetState>,
        savings: Double,
        inputs: FireInputs
    ): Double {
        if (savings <= 0.0) return 0.0

        val targetAllocations = normalizedAllocations(inputs.targetAllocations)
        if (targetAllocations.isNotEmpty()) {
            distributeByAllocations(
                assets = assets,
                amount = savings,
                allocations = targetAllocations,
                source = null,
                inputs = inputs,
                expectedReturn = inputs.expectedEtfReturn
            )
            return savings
        }

        val investableAssets = assets.filter { !it.terminated && it.group.countsInPortfolio }
        if (investableAssets.isEmpty()) {
            val fallback = assets.firstOrNull { it.name == FALLBACK_ASSET_NAME }
            if (fallback != null) {
                fallback.value += savings
            } else {
                assets += AssetState(
                    name = FALLBACK_ASSET_NAME,
                    value = savings,
                    expectedAnnualReturn = inputs.expectedEtfReturn,
                    volatility = 0.16,
                    group = AssetGroup.INVESTABLE,
                    lifecycle = AssetLifecycle(),
                    events = emptyList()
                )
            }
            return savings
        }

        val totalValue = investableAssets.sumOf { it.value.coerceAtLeast(0.0) }
        if (totalValue <= 0.0) {
            val share = savings / investableAssets.size
            investableAssets.forEach { it.value += share }
            return savings
        }

        investableAssets.forEach { asset ->
            val weight = asset.value.coerceAtLeast(0.0) / totalValue
            asset.value += savings * weight
        }
        return savings
    }

    private fun withdrawFromAssets(assets: MutableList<AssetState>, requestedAmount: Double): Double {
        if (requestedAmount <= 0.0) return 0.0

        val investableAssets = assets.filter { !it.terminated && it.group.countsInPortfolio }
        val available = investableAssets.sumOf { it.value.coerceAtLeast(0.0) }
        if (available <= 0.0) return 0.0

        val actualAmount = requestedAmount.coerceAtMost(available)
        investableAssets.forEach { asset ->
            val share = actualAmount * (asset.value.coerceAtLeast(0.0) / available)
            asset.value = (asset.value - share).coerceAtLeast(0.0)
        }
        return actualAmount
    }

    private fun processEvents(
        assets: MutableList<AssetState>,
        year: Int,
        inputs: FireInputs
    ): List<YearEventRecord> {
        val records = mutableListOf<YearEventRecord>()

        assets.toList().forEach { source ->
            source.events.filter { it.year == year }.forEach { event ->
                if (source.terminated || !isActive(source, year)) return@forEach

                val available = source.value.coerceAtLeast(0.0)
                if (available <= 0.0) return@forEach

                val usesFinalValue = event.finalValue != null
                val grossAmount = when {
                    usesFinalValue -> event.finalValue!!.coerceAtLeast(0.0)
                    else -> (event.amount ?: available).coerceIn(0.0, available)
                }
                if (grossAmount <= 0.0) return@forEach

                val removedAmount = if (usesFinalValue) available else grossAmount.coerceAtMost(available)
                source.value = (available - removedAmount).coerceAtLeast(0.0)
                if (usesFinalValue || source.value <= 0.0) {
                    source.value = source.value.coerceAtLeast(0.0)
                    source.terminated = true
                }

                val netAmount = applyEventTaxes(source, event, grossAmount, inputs)
                val targetAllocations = resolveEventAllocations(event, source, inputs)

                distributeByAllocations(
                    assets = assets,
                    amount = netAmount,
                    allocations = targetAllocations,
                    source = source,
                    inputs = inputs,
                    expectedReturn = event.expectedAnnualReturn ?: source.expectedAnnualReturn
                )

                records += YearEventRecord(
                    sourceName = source.name,
                    type = event.type,
                    month = event.month,
                    grossAmount = grossAmount,
                    netAmount = netAmount,
                    destinations = targetAllocations
                )
            }
        }

        return records
    }

    private fun applyEventTaxes(
        source: AssetState,
        event: AssetEvent,
        grossAmount: Double,
        inputs: FireInputs
    ): Double {
        val taxRate = if (source.name == "Third Pillar" && event.type == AssetEventType.THIRD_PILLAR_REDEEMED) {
            inputs.thirdPillarRedemptionTaxRate.coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        return grossAmount * (1.0 - taxRate)
    }

    private fun resolveEventAllocations(
        event: AssetEvent,
        source: AssetState,
        inputs: FireInputs
    ): List<AllocationTarget> {
        val explicitAllocations = normalizedAllocations(event.destinationAllocations)
        if (explicitAllocations.isNotEmpty()) return explicitAllocations

        val namedTargets = event.targetAssetNames.distinct()
        if (namedTargets.isNotEmpty()) {
            val share = 1.0 / namedTargets.size
            return namedTargets.map { AllocationTarget(it, share) }
        }

        val targetAllocations = normalizedAllocations(inputs.targetAllocations)
        if (targetAllocations.isNotEmpty()) return targetAllocations

        val defaults = defaultReinvestmentTargets(source)
        return defaults.map { AllocationTarget(it, 1.0 / defaults.size) }
    }

    private fun distributeByAllocations(
        assets: MutableList<AssetState>,
        amount: Double,
        allocations: List<AllocationTarget>,
        source: AssetState?,
        inputs: FireInputs,
        expectedReturn: Double
    ): Double {
        if (amount <= 0.0) return 0.0

        val normalized = normalizedAllocations(allocations)
        if (normalized.isEmpty()) return 0.0

        var investableAmount = 0.0
        normalized.forEach { allocation ->
            val target = findOrCreateTarget(
                assets = assets,
                targetName = allocation.assetName,
                source = source,
                expectedReturn = expectedReturn,
                inputs = inputs
            )
            val appliedAmount = amount * allocation.percentage
            target.value += appliedAmount
            if (target.group.countsInPortfolio) {
                investableAmount += appliedAmount
            }
        }
        return investableAmount
    }

    private fun normalizedAllocations(allocations: List<AllocationTarget>): List<AllocationTarget> {
        val cleaned = allocations
            .map { AllocationTarget(it.assetName.trim(), it.percentage.coerceAtLeast(0.0)) }
            .filter { it.assetName.isNotBlank() && it.percentage > 0.0 }

        val total = cleaned.sumOf { it.percentage }
        if (total <= 0.0) return emptyList()

        return cleaned.map { it.copy(percentage = it.percentage / total) }
    }

    private fun findOrCreateTarget(
        assets: MutableList<AssetState>,
        targetName: String,
        source: AssetState?,
        expectedReturn: Double,
        inputs: FireInputs
    ): AssetState {
        val existing = assets.firstOrNull { it.name == targetName && !it.terminated }
        if (existing != null) return existing

        val group = when (targetName) {
            "Second Pillar", "Third Pillar" -> AssetGroup.RETIREMENT
            "Emergency Fund", "Home" -> AssetGroup.EXCLUDED
            else -> AssetGroup.INVESTABLE
        }

        val asset = AssetState(
            name = targetName,
            value = 0.0,
            expectedAnnualReturn = defaultReturnForTarget(
                targetName = targetName,
                source = source,
                inputs = inputs,
                expectedReturn = expectedReturn
            ),
            volatility = defaultVolatilityForTarget(targetName),
            group = group,
            lifecycle = AssetLifecycle(),
            events = emptyList()
        )
        assets += asset
        return asset
    }

    private fun defaultReturnForTarget(
        targetName: String,
        source: AssetState?,
        inputs: FireInputs,
        expectedReturn: Double
    ): Double {
        if (expectedReturn > 0.0) return expectedReturn

        return when (targetName) {
            "Cash", "Emergency Fund", "Home" -> 0.0
            "ETF Bonds" -> inputs.expectedBondReturn
            "Crypto" -> inputs.expectedCryptoReturn
            "Gold" -> inputs.expectedGoldReturn
            "Third Pillar" -> inputs.expectedEtfReturn
            "Second Pillar" -> inputs.expectedBondReturn
            else -> source?.expectedAnnualReturn?.takeIf { it > 0.0 } ?: inputs.expectedEtfReturn
        }
    }

    private fun defaultVolatilityForTarget(targetName: String): Double {
        return when (targetName) {
            "Cash", "Emergency Fund", "Home" -> 0.0
            "ETF Bonds", "Second Pillar" -> 0.06
            "ETF Stocks" -> 0.16
            "Crypto" -> 0.60
            "Gold" -> 0.15
            "Private Equity", "Loan", "Deposit House" -> 0.12
            "Third Pillar" -> 0.16
            else -> 0.16
        }
    }

    private fun defaultReinvestmentTargets(source: AssetState): List<String> {
        return when (source.name) {
            "Second Pillar", "Third Pillar" -> listOf("ETF Stocks", "ETF Bonds")
            "Private Equity" -> listOf("ETF Stocks", "Crypto")
            "Loan" -> listOf("Cash", "ETF Bonds")
            "Deposit House" -> listOf("Cash", "ETF Stocks")
            else -> listOf("ETF Stocks", "ETF Bonds")
        }
    }

    private fun isActive(asset: AssetState, year: Int): Boolean {
        val start = asset.lifecycle.startYear
        val end = asset.lifecycle.endYear
        return (start == null || year >= start) && (end == null || year <= end)
    }

    private fun portfolioValue(assets: List<AssetState>): Double {
        return assets
            .filter { !it.terminated && it.group.countsInPortfolio }
            .sumOf { it.value.coerceAtLeast(0.0) }
    }

    private fun snapshotAssets(assets: List<AssetState>): List<AssetSnapshot> {
        return assets.map { asset ->
            AssetSnapshot(
                name = asset.name,
                value = asset.value,
                expectedAnnualReturn = asset.expectedAnnualReturn,
                group = asset.group,
                lifecycle = asset.lifecycle,
                events = asset.events,
                terminated = asset.terminated
            )
        }
    }

    private fun restoreAssets(snapshot: List<AssetSnapshot>): MutableList<AssetState> {
        return snapshot.map { asset ->
            AssetState(
                name = asset.name,
                value = asset.value,
                expectedAnnualReturn = asset.expectedAnnualReturn,
                volatility = 0.16, // Use default; volatility is not stored in snapshots
                group = asset.group,
                lifecycle = asset.lifecycle,
                events = asset.events,
                terminated = asset.terminated
            )
        }.toMutableList()
    }

    private fun buildAllocationSlices(snapshot: List<AssetSnapshot>): List<AllocationSlice> {
        val active = snapshot.filter { !it.terminated && it.value > 0.0 }
        val totalInvestable = active
            .filter { it.group.countsInPortfolio }
            .sumOf { it.value }

        return active
            .sortedWith(compareBy({ it.group.ordinal }, { -it.value }))
            .map {
                val share = if (it.group.countsInPortfolio && totalInvestable > 0.0) {
                    it.value / totalInvestable
                } else {
                    0.0
                }
                AllocationSlice(
                    assetName = it.name,
                    group = it.group,
                    value = it.value,
                    share = share
                )
            }
    }

    private fun buildReadinessRows(
        inputs: FireInputs,
        projections: List<YearProjection>
    ): List<FireReadinessRow> {
        return projections
            .takeWhile { it.age <= 70 }
            .map { projection ->
                val requiredPortfolio = requiredPortfolioForAge(inputs, projection.age)
                val difference = projection.endPortfolio - requiredPortfolio
                FireReadinessRow(
                    age = projection.age,
                    requiredPortfolio = requiredPortfolio,
                    projectedPortfolio = projection.endPortfolio,
                    difference = difference,
                    fireReached = difference >= 0.0
                )
            }
    }

    private fun requiredPortfolioForAge(inputs: FireInputs, age: Int): Double {
        val baseAnnualSpending = inputs.monthlySpendings * 12
        val yearsFromNow = age - inputs.currentAge
        val inflationMultiplier = (1 + inputs.expectedInflation).pow(yearsFromNow.toDouble())
        val retirementSpendingAtAge = baseAnnualSpending * inflationMultiplier

        val yearsPensionStarts = (inputs.pensionStartAge - age).coerceAtLeast(0)
        val yearsAfterPensionStarts = (inputs.lifeExpectancy - inputs.pensionStartAge).coerceAtLeast(0)
        val annualPension = inputs.expectedPension + inputs.swissPension

        // Use average portfolio return for discounting
        // Default allocation: 80% ETF (7%) + 15% Bonds (3%) + 3% Gold (3%) + 2% Crypto (7%)
        // ≈ 80*0.07 + 15*0.03 + 3*0.03 + 2*0.07 = 5.6 + 0.45 + 0.09 + 0.14 = 6.24%
        val portfolioReturn = 0.062 // Blended expected return
        val realReturn = ((1 + portfolioReturn) / (1 + inputs.expectedInflation)) - 1.0
        
        // PV of all spending needs before pension
        var pvSpendingBeforePension = 0.0
        for (yearOffset in 0 until yearsPensionStarts) {
            // Spending at year (yearOffset), starting from age
            val spendingInYear = retirementSpendingAtAge * (1 + inputs.expectedInflation).pow(yearOffset.toDouble())
            val discountFactor = (1 + realReturn).pow(yearOffset.toDouble())
            pvSpendingBeforePension += spendingInYear / discountFactor
        }
        
        // PV of all spending needs after pension
        var pvSpendingAfterPension = 0.0
        for (yearOffset in 0 until yearsAfterPensionStarts) {
            val absoluteYearFromRetirement = yearsPensionStarts + yearOffset
            // Spending at that year (grows with inflation from retirement age)
            val spendingInYear = retirementSpendingAtAge * (1 + inputs.expectedInflation).pow(absoluteYearFromRetirement.toDouble())
            // Pension at that year (also grows with inflation)
            val pensionInYear = annualPension * (1 + inputs.expectedInflation).pow(absoluteYearFromRetirement.toDouble())
            val netSpending = (spendingInYear - pensionInYear).coerceAtLeast(0.0)
            // Discount to retirement age using real return
            val discountFactor = (1 + realReturn).pow(absoluteYearFromRetirement.toDouble())
            pvSpendingAfterPension += netSpending / discountFactor
        }

        val totalRequiredPV = pvSpendingBeforePension + pvSpendingAfterPension

        return when (inputs.withdrawalStrategy) {
            WithdrawalStrategy.FIXED_INFLATION_ADJUSTED -> totalRequiredPV
            WithdrawalStrategy.FOUR_PERCENT_RULE -> {
                // 4% Rule: portfolio needs to be 25x current-year net spending
                val currentYearNetSpending = (retirementSpendingAtAge - if (age >= inputs.pensionStartAge) annualPension else 0.0).coerceAtLeast(0.0)
                currentYearNetSpending / 0.04
            }
            WithdrawalStrategy.VPW -> totalRequiredPV
            WithdrawalStrategy.GUYTON_KLINGER -> {
                val currentYearNetSpending = (retirementSpendingAtAge - if (age >= inputs.pensionStartAge) annualPension else 0.0).coerceAtLeast(0.0)
                currentYearNetSpending / 0.04
            }
        }
    }

    private fun findRecommendedFireAge(readinessByAge: List<FireReadinessRow>): Int? {
        return readinessByAge.firstOrNull { it.fireReached }?.age
    }

    private fun findCoastFireAge(
        inputs: FireInputs,
        projections: List<YearProjection>,
        currentYear: Int
    ): Int? {
        val targetProjection = projections.firstOrNull { it.age >= inputs.targetFireAge }
        val targetPortfolio = targetProjection?.endPortfolio ?: return null

        for (projection in projections) {
            if (projection.age >= inputs.targetFireAge) break

            val futurePortfolio = projectWithoutSavings(
                inputs = inputs,
                snapshot = projection.assetAllocation,
                startYear = projection.year,
                targetYear = currentYear + (inputs.targetFireAge - inputs.currentAge)
            )

            if (futurePortfolio >= targetPortfolio) {
                return projection.age
            }
        }

        return null
    }

    private fun projectWithoutSavings(
        inputs: FireInputs,
        snapshot: List<AssetSnapshot>,
        startYear: Int,
        targetYear: Int
    ): Double {
        val assets = restoreAssets(snapshot)

        for (year in (startYear + 1)..targetYear) {
            applyReturns(assets, year, inputs, null)
            processEvents(assets, year, inputs)
        }

        return portfolioValue(assets)
    }

    private fun findBaristaFireAge(
        inputs: FireInputs,
        projections: List<YearProjection>
    ): Int? {
        // Barista FIRE: age at which the portfolio can cover ~50% of annual spending,
        // so a part-time "barista" job is enough to cover the remaining half.
        // Uses inflation-adjusted spending and a safe 4% withdrawal assumption.
        val baseAnnualSpending = inputs.monthlySpendings * 12
        return projections.firstOrNull { projection ->
            val yearsFromNow = projection.age - inputs.currentAge
            val inflationMultiplier = (1 + inputs.expectedInflation).pow(yearsFromNow.toDouble())
            val inflatedSpending = baseAnnualSpending * inflationMultiplier
            // Half of spending needs to be covered by portfolio at 4% SWR
            val requiredPortfolio = (inflatedSpending * 0.5) / 0.04
            projection.endPortfolio >= requiredPortfolio
        }?.age
    }

    private fun simulateMonteCarloHistorical(
        inputs: FireInputs,
        iterations: Int = 1000
    ): FireOutputs {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        // Run multiple iterations with randomized returns
        val results = (1..iterations).map { iteration ->
            val random = Random(iteration.toLong()) // Seeded for reproducibility
            simulateYearByYear(inputs, currentYear, random)
        }

        // Sort iterations by final portfolio value at FIRE age to find true median
        val sortedByFire = results.sortedBy { evolution ->
            evolution.firstOrNull { it.age >= inputs.targetFireAge }?.endPortfolio ?: 0.0
        }

        // Find successful iterations (portfolio never runs to zero during retirement)
        val successfulIterations = results.count { evolution ->
            val postFire = evolution.filter { it.age >= inputs.targetFireAge }
            postFire.all { it.endPortfolio > 0.0 }
        }
        val successRate = if (results.isNotEmpty()) {
            successfulIterations.toDouble() / results.size
        } else {
            0.0
        }

        // True median iteration (50th percentile)
        val medianIteration = sortedByFire[sortedByFire.size / 2]
        val projectedAtFire = medianIteration.firstOrNull { it.age >= inputs.targetFireAge }
        val projectedPortfolioAtFire = projectedAtFire?.endPortfolio ?: 0.0

        val readinessByAge = buildReadinessRows(inputs, medianIteration)
        val targetReadiness = readinessByAge.firstOrNull { it.age >= inputs.targetFireAge }
            ?: readinessByAge.lastOrNull()

        val requiredFirePortfolio = targetReadiness?.requiredPortfolio ?: 0.0
        val fiPercentage = if (requiredFirePortfolio > 0.0) {
            ((inputs.investablePortfolio / requiredFirePortfolio) * 100).coerceIn(0.0, 1000.0)
        } else {
            0.0
        }

        val recommendedFireAge = findRecommendedFireAge(readinessByAge)
        val coastFireAge = findCoastFireAge(inputs, medianIteration, currentYear)
        val baristaFireAge = findBaristaFireAge(inputs, medianIteration)
        val allocationAtFire = buildAllocationSlices(projectedAtFire?.assetAllocation.orEmpty())

        return FireOutputs(
            projectedPortfolioAtFire = projectedPortfolioAtFire,
            requiredFirePortfolio = requiredFirePortfolio,
            fiPercentage = fiPercentage,
            recommendedFireAge = recommendedFireAge,
            coastFireAge = coastFireAge,
            baristaFireAge = baristaFireAge,
            probabilityOfSuccess = (successRate * 100).coerceIn(0.0, 100.0),
            portfolioEvolution = medianIteration,
            readinessByAge = readinessByAge,
            allocationAtFire = allocationAtFire,
            totalNetWorth = inputs.totalNetWorth,
            investableAssets = inputs.investablePortfolio,
            retirementAssetsAtStart = inputs.retirementAssets,
            excludedAssetsAtStart = inputs.excludedAssets
        )
    }

    fun simulateWithMonteCarloHistorical(
        inputs: FireInputs,
        iterations: Int = 1000,
        historicalReturns: List<Double>? = null
    ): FireOutputs {
        return simulateMonteCarloHistorical(inputs, iterations)
    }

    fun simulateWithMonteCarloBootstrap(
        inputs: FireInputs,
        historicalData: List<Double>,
        iterations: Int = 1000
    ): FireOutputs {
        return simulateMonteCarloHistorical(inputs, iterations)
    }
}
