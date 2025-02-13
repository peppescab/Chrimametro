/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.split

data class SplitModel(val name: String, val totalSum: Int, val listInvestors: List<Investor>, val incomePercentage: Int)

data class Investor(val name: String, val quote: Int)
