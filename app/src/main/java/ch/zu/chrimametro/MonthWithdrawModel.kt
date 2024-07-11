/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro

data class MonthWithdrawModel(val name: String, val expenses: MutableList<Float>) {

    fun getTotal(): Double {
        var expens = 0.0
        expenses.forEach {
            expens += it
        }
        return expens
    }
}
