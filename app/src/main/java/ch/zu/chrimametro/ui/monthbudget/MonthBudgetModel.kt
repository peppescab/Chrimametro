/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.monthbudget

import ch.zu.chrimametro.Utils.sumOfNullable
import ch.zu.chrimametro.ui.budget.BudgetModel

data class MonthBudget(
    var budgteItemList: List<BudgetModel> = emptyList(),
    var nameMonth: String,
    var totalAmount: Int,
    var currentAmount: Int,
) {
    fun calculateSum() {
        totalAmount = if (budgteItemList.isEmpty()) totalAmount else budgteItemList.sumOf { it.moneyAdded }
        currentAmount = if (budgteItemList.isEmpty()) totalAmount else budgteItemList.sumOfNullable { it?.actualValue }
    }

    init {
        calculateSum()
    }
}
