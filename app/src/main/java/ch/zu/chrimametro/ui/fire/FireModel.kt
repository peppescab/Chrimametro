/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
 */
package ch.zu.chrimametro.ui.fire

import java.util.Calendar

enum class AssetGroup(val displayName: String, val countsInPortfolio: Boolean) {
    INVESTABLE("Investable Assets", true),
    RETIREMENT("Retirement Assets", false),
    EXCLUDED("Excluded Assets", false)
}

enum class AssetEventType {
    PRIVATE_EQUITY_MATURITY,
    LOAN_FINISHED,
    DEPOSIT_HOUSE_REFUNDED,
    FINPENSION_CLOSED,
    THIRD_PILLAR_REDEEMED,
    REINVESTMENT,
    TRANSFER
}

enum class ThirdPillarStrategy(val displayName: String) {
    KEEP_UNTIL_RETIREMENT("Keep until retirement"),
    REDEEM_WHEN_LEAVING_SWITZERLAND("Redeem when leaving Switzerland")
}

enum class WithdrawalStrategy(val displayName: String) {
    FIXED_INFLATION_ADJUSTED("Fixed spending (inflation-adjusted)"),
    FOUR_PERCENT_RULE("4% Rule"),
    VPW("Variable Percentage Withdrawal (VPW)"),
    GUYTON_KLINGER("Guyton-Klinger guardrails")
}

enum class SimulationMode(val displayName: String) {
    DETERMINISTIC("Deterministic"),
    MONTE_CARLO("Monte Carlo (WIP)")
}

data class AllocationTarget(
    val assetName: String,
    val percentage: Double = 0.0
)

data class AssetLifecycle(
    val startYear: Int? = null,
    val endYear: Int? = null
)

data class AssetEvent(
    val year: Int,
    val month: Int? = null,
    val type: AssetEventType,
    val amount: Double? = null,
    val finalValue: Double? = null,
    val targetAssetNames: List<String> = emptyList(),
    val destinationAllocations: List<AllocationTarget> = emptyList(),
    val expectedAnnualReturn: Double? = null
)

data class AssetType(
    val name: String,
    val currentValue: Double = 0.0,
    val expectedAnnualReturn: Double = 0.0,
    val volatility: Double = 0.0,
    val group: AssetGroup = AssetGroup.INVESTABLE,
    val lifecycle: AssetLifecycle = AssetLifecycle(),
    val events: List<AssetEvent> = emptyList()
)

private fun defaultTargetAllocations(): List<AllocationTarget> {
    return listOf(
        AllocationTarget(assetName = "ETF Stocks", percentage = 0.80),
        AllocationTarget(assetName = "ETF Bonds", percentage = 0.15),
        AllocationTarget(assetName = "Gold", percentage = 0.03),
        AllocationTarget(assetName = "Crypto", percentage = 0.02)
    )
}

private fun defaultFireAssets(): List<AssetType> {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    return listOf(
        AssetType(name = "Cash", currentValue = 2288.0, expectedAnnualReturn = 0.0, group = AssetGroup.INVESTABLE),
        AssetType(name = "ETF Stocks", currentValue = 12437.0, expectedAnnualReturn = 0.07, volatility = 0.16, group = AssetGroup.INVESTABLE),
        AssetType(name = "ETF Bonds", currentValue = 9604.0, expectedAnnualReturn = 0.03, volatility = 0.06, group = AssetGroup.INVESTABLE),
        AssetType(name = "Crypto", currentValue = 4833.0, expectedAnnualReturn = 0.07, volatility = 0.60, group = AssetGroup.INVESTABLE),
        AssetType(name = "Gold", currentValue = 1401.0, expectedAnnualReturn = 0.03, volatility = 0.15, group = AssetGroup.INVESTABLE),
        AssetType(
            name = "Private Equity",
            currentValue = 67500.0,
            expectedAnnualReturn = 0.12,
            group = AssetGroup.INVESTABLE,
            events = listOf(
                AssetEvent(
                    year = currentYear + 1,
                    month = 10,
                    type = AssetEventType.PRIVATE_EQUITY_MATURITY,
                    finalValue = 49170.0,
                    destinationAllocations = listOf(
                        AllocationTarget("ETF Stocks", 0.80),
                        AllocationTarget("ETF Bonds", 0.15),
                        AllocationTarget("Gold", 0.03),
                        AllocationTarget("Crypto", 0.02)
                    )
                )
            )
        ),
        AssetType(
            name = "Loan",
            currentValue = 9857.0,
            expectedAnnualReturn = 0.04,
            group = AssetGroup.INVESTABLE,
            events = listOf(
                AssetEvent(
                    year = currentYear + 2,
                    type = AssetEventType.LOAN_FINISHED,
                    destinationAllocations = listOf(
                        AllocationTarget("ETF Bonds", 0.70),
                        AllocationTarget("Cash", 0.30)
                    )
                )
            )
        ),
        AssetType(
            name = "Deposit House",
            currentValue = 7402.0,
            expectedAnnualReturn = 0.0,
            group = AssetGroup.INVESTABLE,
            events = listOf(
                // Year is overridden at simulation time to match returnToItalyYear.
                AssetEvent(
                    year = currentYear + 3,
                    type = AssetEventType.DEPOSIT_HOUSE_REFUNDED,
                    destinationAllocations = listOf(
                        AllocationTarget("ETF Stocks", 0.80),
                        AllocationTarget("ETF Bonds", 0.15),
                        AllocationTarget("Gold", 0.03),
                        AllocationTarget("Crypto", 0.02)
                    )
                )
            )
        ),
        AssetType(
            name = "Second Pillar",
            currentValue = 46348.0,
            expectedAnnualReturn = 0.04,
            group = AssetGroup.RETIREMENT,
            events = listOf(
                AssetEvent(
                    year = currentYear + 24,
                    type = AssetEventType.FINPENSION_CLOSED
                )
            )
        ),
        AssetType(
            name = "Third Pillar",
            currentValue = 32177.0,
            expectedAnnualReturn = 0.07,
            group = AssetGroup.RETIREMENT
        ),
        AssetType(
            name = "Emergency Fund",
            currentValue = 10000.0,
            expectedAnnualReturn = 0.0,
            group = AssetGroup.EXCLUDED
        ),
        AssetType(
            name = "Home",
            currentValue = 133000.0,
            expectedAnnualReturn = 0.0,
            group = AssetGroup.EXCLUDED
        )
    )
}

// 10-year averages derived from public API data:
// - Italy inflation: World Bank CPI, 2016-2025
// - ETF / Bond / Gold: Yahoo Finance adjusted-close CAGR, 2016-2025
// - Crypto: Yahoo Finance close CAGR for BTC-USD, 2016-2025
private const val DEFAULT_ITALY_INFLATION_10Y = 0.02095485240618569
private const val DEFAULT_ETF_RETURN_10Y = 0.1530282963755536
private const val DEFAULT_BOND_RETURN_10Y = 0.01849318985005688
private const val DEFAULT_CRYPTO_RETURN_10Y = 0.7279452669235245
private const val DEFAULT_GOLD_RETURN_10Y = 0.13994900849227543

data class FireInputs(
    val currentAge: Int = 43,
    val targetFireAge: Int = 50,
    val lifeExpectancy: Int = 90,
    val returnToItalyYear: Int = 2028,
    val swissYearlySavings: Double = 40000.0,
    val italianYearlySavings: Double = 20000.0,
    val monthlySpendings: Double = 2000.0,
    val expectedInflation: Double = DEFAULT_ITALY_INFLATION_10Y,
    val expectedEtfReturn: Double = DEFAULT_ETF_RETURN_10Y,
    val expectedBondReturn: Double = DEFAULT_BOND_RETURN_10Y,
    val expectedCryptoReturn: Double = DEFAULT_CRYPTO_RETURN_10Y,
    val expectedGoldReturn: Double = DEFAULT_GOLD_RETURN_10Y,
    val swissPensionStartAge: Int = 65,
    val swissPension: Double = 3840.0,
    val combinedPensionStartAge: Int = 69,
    val combinedPension: Double = 24000.0,
    val portfolioCurrency: String = "CHF",
    val thirdPillarStrategy: ThirdPillarStrategy = ThirdPillarStrategy.KEEP_UNTIL_RETIREMENT,
    val thirdPillarRedemptionTaxRate: Double = 0.06,
    val targetAllocations: List<AllocationTarget> = defaultTargetAllocations(),
    val withdrawalStrategy: WithdrawalStrategy = WithdrawalStrategy.FIXED_INFLATION_ADJUSTED,
    val simulationMode: SimulationMode = SimulationMode.DETERMINISTIC,
    val monteCarloIterations: Int = 1000,
    val assets: List<AssetType> = defaultFireAssets()
)

val FireInputs.investablePortfolio: Double
    get() = assets
        .filter { it.group.countsInPortfolio }
        .sumOf { it.currentValue.coerceAtLeast(0.0) }

val FireInputs.retirementAssets: Double
    get() = assets
        .filter { it.group == AssetGroup.RETIREMENT }
        .sumOf { it.currentValue.coerceAtLeast(0.0) }

val FireInputs.excludedAssets: Double
    get() = assets
        .filter { it.group == AssetGroup.EXCLUDED }
        .sumOf { it.currentValue.coerceAtLeast(0.0) }

val FireInputs.totalNetWorth: Double
    get() = assets.sumOf { it.currentValue.coerceAtLeast(0.0) }

data class AssetSnapshot(
    val name: String,
    val value: Double,
    val expectedAnnualReturn: Double = 0.0,
    val group: AssetGroup = AssetGroup.INVESTABLE,
    val lifecycle: AssetLifecycle = AssetLifecycle(),
    val events: List<AssetEvent> = emptyList(),
    val terminated: Boolean = false
)

data class YearEventRecord(
    val sourceName: String,
    val type: AssetEventType,
    val month: Int? = null,
    val grossAmount: Double,
    val netAmount: Double,
    val destinations: List<AllocationTarget> = emptyList()
) {
    val displayLabel: String
        get() {
            val monthLabel = month?.let { monthShortName(it) + " " } ?: ""
            val destLabel = destinations.joinToString(", ") {
                "${(it.percentage * 100).toInt()}% ${it.assetName}"
            }
            return if (destLabel.isNotBlank()) {
                "${monthLabel}${sourceName} → $destLabel"
            } else {
                "${monthLabel}${sourceName}"
            }
        }
}

private fun monthShortName(month: Int): String {
    return when (month) {
        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
        5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
        9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
        else -> ""
    }
}

data class YearProjection(
    val year: Int,
    val age: Int,
    val startPortfolio: Double = 0.0,
    val contributions: Double = 0.0,
    val returns: Double = 0.0,
    val withdrawals: Double = 0.0,
    val endPortfolio: Double = 0.0,
    val savings: Double = 0.0,
    val spending: Double = 0.0,
    val inflationAdjustedSpending: Double = 0.0,
    val cumulativeContributions: Double = 0.0,
    val cumulativeReturns: Double = 0.0,
    val pensionIncome: Double = 0.0,
    val assetAllocation: List<AssetSnapshot> = emptyList(),
    val events: List<YearEventRecord> = emptyList(),
    val balanceError: Double = 0.0
)

data class FireReadinessRow(
    val age: Int,
    val requiredPortfolio: Double = 0.0,
    val projectedPortfolio: Double = 0.0,
    val difference: Double = 0.0,
    val fireReached: Boolean = false
)

data class AllocationSlice(
    val assetName: String,
    val group: AssetGroup,
    val value: Double,
    val share: Double
)

data class FireOutputs(
    val projectedPortfolioAtFire: Double = 0.0,
    val requiredFirePortfolio: Double = 0.0,
    val fiPercentage: Double = 0.0,
    val recommendedFireAge: Int? = null,
    val coastFireAge: Int? = null,
    val baristaFireAge: Int? = null,
    val probabilityOfSuccess: Double = 0.0,
    val portfolioEvolution: List<YearProjection> = emptyList(),
    val readinessByAge: List<FireReadinessRow> = emptyList(),
    val allocationAtFire: List<AllocationSlice> = emptyList(),
    val totalNetWorth: Double = 0.0,
    val investableAssets: Double = 0.0,
    val retirementAssetsAtStart: Double = 0.0,
    val excludedAssetsAtStart: Double = 0.0
)

data class FireSimulationState(
    val inputs: FireInputs = FireInputs(),
    val outputs: FireOutputs = FireOutputs(),
    val isLoading: Boolean = false,
    val error: String? = null
)
