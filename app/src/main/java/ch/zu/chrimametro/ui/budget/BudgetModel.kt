/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.budget

import ch.zu.chrimametro.R

data class BudgetSpecificItem(val nameItem: String, val moneyAdd: Int, val conversionRate: Int = 1)

data class BudgetModel(
    val nameBudget: String,
    var moneyAdded: Int = 0,
    var actualValue: Int? = null,
    val toBeTaxed: Boolean,
    val levelOfRisk: Float,
    val currencyChf: Boolean = false,
    val specificItemList: MutableList<BudgetSpecificItem> = mutableListOf(),
    val type: BudgetType = BudgetType.Various
) {
    fun calculateSum() {
        moneyAdded =
            if (specificItemList.isEmpty()) moneyAdded else specificItemList.sumOf { it.moneyAdd * it.conversionRate }
        if (actualValue == null || actualValue == 0 || specificItemList.isNotEmpty()) actualValue = moneyAdded
    }

    init {
        calculateSum()
    }
}

enum class BudgetType(val imgResource: Int) {
    Bank(R.drawable.ic_bank),
    Cash(R.drawable.ic_cash),
    Crypto(R.drawable.ic_crypto),
    ETF(R.drawable.ic_bull),
    RealEstate(R.drawable.ic_real_estate),
    Swiss(R.drawable.ic_swiss_flag),
    Various(R.drawable.ic_various)
}

enum class Currency(val imgResource: Int) {
    Euro(R.drawable.ic_euro),
    Chf(R.drawable.ic_swiss_flag)
}
