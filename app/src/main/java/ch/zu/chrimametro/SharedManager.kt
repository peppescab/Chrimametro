/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro

import android.content.Context
import android.content.SharedPreferences
import ch.zu.chrimametro.Utils.fromListToMap
import ch.zu.chrimametro.Utils.fromMapToList
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferenceManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    }
    private val gson = Gson()

    fun removeMonthWithdrawList(monthName: String, valueToAdd: Float) {
        val loadedList = loadMonthWithdrawList()
        val monthWithdrawModel = loadedList.find { it.name == monthName }
        // If the MonthWithdrawModel object is found, add the expense to its expenses list
        monthWithdrawModel?.expenses?.remove(valueToAdd)
        val json = gson.toJson(loadedList)
        sharedPreferences.edit().putString("monthWithdrawList", json).apply()
    }

    fun saveMonthWithdrawList(monthName: String, valueToAdd: Float? = null) {
        val loadedList = loadMonthWithdrawList().toMutableList()
        val monthWithdrawModel = loadedList.find { it.name == monthName }
        // If the MonthWithdrawModel object is found, add the expense to its expenses list
        valueToAdd?.let {
            monthWithdrawModel?.expenses?.add(it)
        } ?: run { loadedList.add(0, MonthWithdrawModel(monthName, mutableListOf())) }
        val json = gson.toJson(loadedList)
        sharedPreferences.edit().putString("monthWithdrawList", json).apply()
    }

    fun loadMonthWithdrawList(): List<MonthWithdrawModel> {
        return sharedPreferences.getString("monthWithdrawList", null)?.let { json ->
            val type = object : TypeToken<List<MonthWithdrawModel>>() {}.type
            gson.fromJson(json, type)
        } ?: stubList
    }
}

val stubList = listOf(
    MonthWithdrawModel("Feb 2024", mutableListOf(999f, 903f)),
    MonthWithdrawModel("Jan 2024", mutableListOf(130f, 999f)),
    MonthWithdrawModel("Dec 2023", mutableListOf(999f, 800f, 1000f)),
    MonthWithdrawModel("Nov 2023", mutableListOf(999f, 800f, 1000f)),
)


