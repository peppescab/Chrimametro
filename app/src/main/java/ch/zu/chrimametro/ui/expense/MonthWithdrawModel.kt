/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.expense

data class MonthWithdrawModel(
    val name: String,
    val expenses: MutableList<Float>,
    val listNote: MutableList<String> = mutableListOf(),
    val salary: Float,
    val fixedCosts: Float
) {
    fun getTotal(): Double {
        var expens = 0.0
        expenses.forEach {
            expens += it
        }
        return expens
    }

    fun getNet(): Double {
        return salary - getTotal() - fixedCosts
    }

    fun getPercentageCashFlow(): Float {
        val totalExpenses = getTotal() + fixedCosts
        val net = salary - totalExpenses
        return ((net / salary) * 100).toFloat()
    }

    fun getAverage(): Float = if (expenses.isEmpty()) 0f else expenses.sum() / expenses.size
}

