/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.expense

data class MonthWithdrawModel(
    val name: String, val expenses: MutableList<Float>,
    val listNote: MutableList<String> = mutableListOf()
) {
    fun getTotal(): Double {
        var expens = 0.0
        expenses.forEach {
            expens += it
        }
        return expens
    }
}
