package ch.zu.chrimametro.ui.fire

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.abs

class FinancialSimulationEngineTest {
    
    private val engine = FinancialSimulationEngine()
    private val EPSILON = 1.0 // Allow 1 CHF rounding error
    
    /**
     * Test 1: Simple compound growth
     * 100,000 CHF at 10% annual return, no contributions, no withdrawals
     * Expected: 110,000 → 121,000 → 133,100
     */
    @Test
    fun testSimpleCompoundGrowth() {
        val inputs = FireInputs(
            currentAge = 40,
            targetFireAge = 43,
            lifeExpectancy = 50,
            monthlySpendings = 0.0,  // No spending
            swissYearlySavings = 0.0,  // No contributions
            italianYearlySavings = 0.0,
            assets = listOf(
                AssetType(
                    name = "Test Fund",
                    currentValue = 100000.0,
                    expectedAnnualReturn = 0.10,
                    volatility = 0.0,
                    group = AssetGroup.INVESTABLE
                )
            )
        )
        
        val outputs = engine.simulate(inputs)
        val projections = outputs.portfolioEvolution
        
        // Year 1 (age 40): 100,000 * 1.10 = 110,000
        val year1 = projections.find { it.age == 40 }
        assertNotNull("Year 1 projection missing", year1)
        assertTrue("Year 1 end portfolio should be ~110,000", 
            abs(year1!!.endPortfolio - 110000.0) < EPSILON)
        
        // Year 2 (age 41): 110,000 * 1.10 = 121,000
        val year2 = projections.find { it.age == 41 }
        assertNotNull("Year 2 projection missing", year2)
        assertTrue("Year 2 end portfolio should be ~121,000", 
            abs(year2!!.endPortfolio - 121000.0) < EPSILON)
        
        // Year 3 (age 42): 121,000 * 1.10 = 133,100
        val year3 = projections.find { it.age == 42 }
        assertNotNull("Year 3 projection missing", year3)
        assertTrue("Year 3 end portfolio should be ~133,100", 
            abs(year3!!.endPortfolio - 133100.0) < EPSILON)
    }
    
    /**
     * Test 2: Linear growth with contributions, no returns
     * 100,000 CHF at 0% return, 20,000/year contributions
     * Expected: 100,000 → 120,000 → 140,000 → 160,000
     */
    @Test
    fun testLinearGrowthWithContributions() {
        val inputs = FireInputs(
            currentAge = 40,
            targetFireAge = 50,  // No withdrawals before this
            lifeExpectancy = 60,
            monthlySpendings = 0.0,  // No withdrawals
            swissYearlySavings = 20000.0,  // 20k/year contribution
            italianYearlySavings = 0.0,
            expectedInflation = 0.0,  // No inflation for simplicity
            expectedEtfReturn = 0.0,  // Override default to 0%
            expectedBondReturn = 0.0,
            expectedCryptoReturn = 0.0,
            returnToItalyYear = 2099,  // Far in future (default 2028 would interfere with test)
            targetAllocations = listOf(
                AllocationTarget("Test Fund", 1.0)  // 100% to our test fund
            ),
            assets = listOf(
                AssetType(
                    name = "Test Fund",
                    currentValue = 100000.0,
                    expectedAnnualReturn = 0.0,  // 0% return
                    volatility = 0.0,
                    group = AssetGroup.INVESTABLE
                )
            )
        )
        
        val outputs = engine.simulate(inputs)
        val projections = outputs.portfolioEvolution
        
        // Year 1 (age 40): 100,000 + 0 returns + 20,000 contrib = 120,000
        val year1 = projections.find { it.age == 40 }
        assertNotNull("Year 1 projection missing", year1)
        assertTrue("Year 1 end portfolio should be ~120,000, got ${year1?.endPortfolio}", 
            abs(year1!!.endPortfolio - 120000.0) < EPSILON)
        
        // Year 2 (age 41): 120,000 + 0 returns + 20,000 contrib = 140,000
        val year2 = projections.find { it.age == 41 }
        assertNotNull("Year 2 projection missing", year2)
        assertTrue("Year 2 end portfolio should be ~140,000, got ${year2?.endPortfolio}", 
            abs(year2!!.endPortfolio - 140000.0) < EPSILON)
        
        // Year 3 (age 42): 140,000 + 0 returns + 20,000 contrib = 160,000
        val year3 = projections.find { it.age == 42 }
        assertNotNull("Year 3 projection missing", year3)
        assertTrue("Year 3 end portfolio should be ~160,000, got ${year3?.endPortfolio}", 
            abs(year3!!.endPortfolio - 160000.0) < EPSILON)
    }
    
    /**
     * Test 3: Accounting identity verification
     * End Portfolio = Start + Returns + Contributions - Withdrawals
     * Must hold for every single year
     */
    @Test
    fun testAccountingIdentity() {
        val inputs = FireInputs(
            currentAge = 40,
            targetFireAge = 45,
            lifeExpectancy = 55,
            monthlySpendings = 2000.0,  // 24k/year
            swissYearlySavings = 30000.0,
            italianYearlySavings = 0.0,
            expectedInflation = 0.02,
            assets = listOf(
                AssetType(
                    name = "ETF",
                    currentValue = 100000.0,
                    expectedAnnualReturn = 0.07,
                    volatility = 0.0,
                    group = AssetGroup.INVESTABLE
                )
            )
        )
        
        val outputs = engine.simulate(inputs)
        val projections = outputs.portfolioEvolution
        
        // Verify formula for each year
        for (i in 0 until projections.size - 1) {
            val projection = projections[i]
            val formula = projection.startPortfolio + 
                         projection.returns + 
                         projection.contributions - 
                         projection.withdrawals
            
            val error = abs(projection.endPortfolio - formula)
            assertTrue(
                "Accounting identity failed at year ${projection.year} (age ${projection.age}): " +
                "End(${projection.endPortfolio}) != Start(${projection.startPortfolio}) + " +
                "Returns(${projection.returns}) + Contrib(${projection.contributions}) - " +
                "Withdraw(${projection.withdrawals}). Error: $error",
                error < EPSILON
            )
        }
    }
    
    /**
     * Test 4: Deterministic returns are never negative
     * In deterministic mode, annual returns should always be >= 0
     */
    @Test
    fun testDeterministicNeverNegative() {
        val inputs = FireInputs(
            currentAge = 40,
            targetFireAge = 50,
            lifeExpectancy = 70,
            monthlySpendings = 1000.0,
            swissYearlySavings = 10000.0,
            italianYearlySavings = 0.0,
            simulationMode = SimulationMode.DETERMINISTIC,
            assets = listOf(
                AssetType(
                    name = "Portfolio",
                    currentValue = 100000.0,
                    expectedAnnualReturn = 0.05,
                    volatility = 0.1,
                    group = AssetGroup.INVESTABLE
                )
            )
        )
        
        val outputs = engine.simulate(inputs)
        val projections = outputs.portfolioEvolution
        
        for (projection in projections) {
            assertTrue(
                "Deterministic mode returned negative returns at age ${projection.age}: " +
                "${projection.returns}",
                projection.returns >= -0.01  // Allow tiny rounding error
            )
        }
    }
    
    /**
     * Test 5: Pension income only starts at pensionStartAge
     * Before pension age: no pension in accounting
     * After pension age: pension reduces required withdrawals
     */
    @Test
    fun testPensionTiming() {
        val inputs = FireInputs(
            currentAge = 40,
            targetFireAge = 50,
            pensionStartAge = 65,
            lifeExpectancy = 80,
            monthlySpendings = 2000.0,
            expectedPension = 15000.0,
            swissPension = 0.0,
            swissYearlySavings = 50000.0,
            italianYearlySavings = 0.0,
            expectedInflation = 0.0,
            assets = listOf(
                AssetType(
                    name = "Portfolio",
                    currentValue = 500000.0,
                    expectedAnnualReturn = 0.05,
                    volatility = 0.0,
                    group = AssetGroup.INVESTABLE
                )
            )
        )
        
        val outputs = engine.simulate(inputs)
        val projections = outputs.portfolioEvolution
        
        // Before pension age (age < 65)
        val beforePension = projections.filter { it.age < 65 }
        for (proj in beforePension) {
            assertTrue(
                "Pension income should be 0 before age 65, found ${proj.pensionIncome} at age ${proj.age}",
                proj.pensionIncome <= 0.01  // Allow tiny rounding
            )
        }
        
        // After pension age (age >= 65)
        val afterPension = projections.filter { it.age >= 65 }
        for (proj in afterPension) {
            assertTrue(
                "Pension income should be 15k at age ${proj.age}, found ${proj.pensionIncome}",
                abs(proj.pensionIncome - 15000.0) < 1.0
            )
        }
    }
    
    /**
     * Test 6: Required FIRE Portfolio considers pension
     * With pension covering spending, required portfolio should be much smaller
     */
    @Test
    fun testRequiredPortfolioWithPension() {
        // Scenario: Retire at 50, spend 24k/year, pension 24k at 67
        // Before pension (ages 50-66): need full spending
        // After pension (ages 67+): need 0 (pension covers all)
        
        val inputs = FireInputs(
            currentAge = 43,
            targetFireAge = 50,
            pensionStartAge = 67,
            lifeExpectancy = 90,
            monthlySpendings = 2000.0,  // 24k/year
            expectedPension = 24000.0,  // Pension covers all spending
            swissPension = 0.0,
            swissYearlySavings = 0.0,
            italianYearlySavings = 0.0,
            expectedInflation = 0.02,
            withdrawalStrategy = WithdrawalStrategy.FIXED_INFLATION_ADJUSTED,
            assets = listOf(
                AssetType(
                    name = "Portfolio",
                    currentValue = 100000.0,
                    expectedAnnualReturn = 0.07,
                    volatility = 0.0,
                    group = AssetGroup.INVESTABLE
                )
            )
        )
        
        val outputs = engine.simulate(inputs)
        
        // Required portfolio should be for ~17 years of spending (50-66)
        // not 40 years (50-90)
        // With 2% inflation and 6.24% return:
        // Spending @ 50: 24k * 1.02^7 ≈ 27.5k
        // PV ≈ 27.5k * annuity(17, 4%) ≈ 355k
        
        assertTrue(
            "Required portfolio should be ~355k-410k for 17 years, got ${outputs.requiredFirePortfolio}",
            outputs.requiredFirePortfolio in 350000.0..420000.0
        )
    }
    
    /**
     * Test 7: No contributions after retirement
     * Contributions should stop at targetFireAge
     */
    @Test
    fun testNoContributionsAfterRetirement() {
        val inputs = FireInputs(
            currentAge = 40,
            targetFireAge = 45,
            lifeExpectancy = 60,
            monthlySpendings = 1000.0,
            swissYearlySavings = 20000.0,
            italianYearlySavings = 0.0,
            expectedInflation = 0.0,
            returnToItalyYear = 2099,  // Avoid early year transition
            targetAllocations = listOf(
                AllocationTarget("Portfolio", 1.0)  // 100% to our test portfolio
            ),
            assets = listOf(
                AssetType(
                    name = "Portfolio",
                    currentValue = 100000.0,
                    expectedAnnualReturn = 0.0,
                    volatility = 0.0,
                    group = AssetGroup.INVESTABLE
                )
            )
        )
        
        val outputs = engine.simulate(inputs)
        val projections = outputs.portfolioEvolution
        
        // Before retirement: savings column should match contributions
        val beforeRetire = projections.filter { it.age < 45 }
        for (proj in beforeRetire) {
            assertTrue(
                "Savings should be recorded before retirement at age ${proj.age}, got ${proj.savings}",
                proj.savings > 0
            )
        }
        
        // After retirement: savings should be 0
        val afterRetire = projections.filter { it.age >= 45 }
        for (proj in afterRetire) {
            assertTrue(
                "Savings should be 0 after retirement at age ${proj.age}, got ${proj.savings}",
                proj.savings <= 0.01
            )
        }
    }
}
