/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro

import ch.zu.chrimametro.ui.expense.MonthWithdrawModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Utils {

    fun fromListToMap(monthWithdrawList: List<MonthWithdrawModel>?): MutableMap<String, List<Float>> {
        val mutableMapToRes = mutableMapOf<String, List<Float>>()
        monthWithdrawList?.forEach {
            mutableMapToRes[it.name] = it.expenses
        }
        return mutableMapToRes
    }

    fun fromMapToList(mapToTrans: Map<String, MutableList<Float>>?): MutableList<MonthWithdrawModel> {
        val listToRes = mutableListOf<MonthWithdrawModel>()
        mapToTrans?.forEach {
            listToRes.add(MonthWithdrawModel(it.key, it.value))
        }
        return listToRes
    }

    fun addMonth(monthS: String): String {
        // Define the date format
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)

        // Parse the input string into a Date object
        val date = dateFormat.parse(monthS)

        // Convert the Date object to a Calendar object for manipulation
        val calendar = Calendar.getInstance()
        calendar.time = date

        // Increment the date by one month
        calendar.add(Calendar.MONTH, 1)

        // Format the updated date and return it
        return dateFormat.format(calendar.time)
    }

    fun getCurrentMonth(): String {
        // Define the date format
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.ENGLISH)

        // Get the current date in the format "MMM yyyy"
        return dateFormat.format(Calendar.getInstance().time)
    }

    inline fun <T> Iterable<T?>.sumOfNullable(selector: (T?) -> Int?): Int {
        var sum: Int = 0
        for (element in this) {
            sum += selector(element) ?: 0
        }
        return sum
    }

    fun calculateThePercentage(currentValue: Double, initialValue: Double): String {
        // Calculate the difference
        val difference = currentValue - initialValue
        // Calculate the percentage change
        val percentageChange = (difference / initialValue) * 100
        // Return the percentage change rounded to three decimal places
        return if (percentageChange == 0.0) "" else String.format("%.3f", percentageChange)
    }
}
