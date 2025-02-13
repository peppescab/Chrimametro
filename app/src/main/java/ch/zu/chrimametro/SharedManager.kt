/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro

import android.content.Context
import android.content.SharedPreferences
import ch.zu.chrimametro.ui.budget.BudgetModel
import ch.zu.chrimametro.ui.budget.BudgetSpecificItem
import ch.zu.chrimametro.ui.earning.EarningModelUiState
import ch.zu.chrimametro.ui.expense.MonthWithdrawModel
import ch.zu.chrimametro.ui.monthbudget.MonthBudget
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

    private val MONTH_LIST = "monthWithdrawList"
    private val MONTH_BUDGET_LIST = "monthBudgetItemL"
    private val UISTATE = "uiEarningState"

    fun removeMonthWithdrawList(monthName: String, amountToRemove: Float? = null, noteToRemove: String? = null) {
        val loadedList = loadMonthWithdrawList()
        val monthWithdrawModel = loadedList.find { it.name == monthName }
        // If the MonthWithdrawModel object is found, add the expense to its expenses list
        if (amountToRemove == null) {
            monthWithdrawModel?.listNote?.remove(noteToRemove)
        } else {
            monthWithdrawModel?.expenses?.remove(amountToRemove)
        }
        storeToMonthList(loadedList)
    }

    fun removeMonth(month: MonthWithdrawModel) {
        storeToMonthList(loadMonthWithdrawList().also {
            it.remove(month)
        })
    }

    fun removeMonthBudget(month: MonthBudget) {
        storeToMonthBudgetList(loadMonthBudgetList().toMutableList().also {
            it.remove(month)
        })
    }

    fun removeBudgetItemList(item: BudgetModel, currentMonth: String) {
        // Load the month budget list and update the corresponding month's budget item list
        storeToMonthBudgetList(
            loadMonthBudgetList().toMutableList().also { monthBudgets ->
                monthBudgets.find { it.nameMonth == currentMonth }?.apply {
                    val updatedList = budgteItemList.toMutableList().apply {
                        remove(item)
                    }
                    budgteItemList = updatedList
                    calculateSum()
                }
            }
        )
    }

    fun saveMonthWithdrawList(monthName: String, valueToAdd: Float? = null, noteToAdd: String? = null) {
        val loadedList = loadMonthWithdrawList().toMutableList()
        val monthWithdrawModel = loadedList.find { it.name == monthName }
        // If the MonthWithdrawModel object is found, add the expense to its expenses list
        valueToAdd?.let {
            monthWithdrawModel?.expenses?.add(it)
        } ?: run {
            if (noteToAdd == null) {
                loadedList.add(0, MonthWithdrawModel(monthName, mutableListOf()))
            } else {
                monthWithdrawModel?.listNote?.add(noteToAdd)
            }
        }
        storeToMonthList(loadedList)
    }

    fun saveBudgetItemList(item: BudgetModel, currentMonth: String): Boolean {
        val listLoaded = loadBudgetItemList(currentMonth).toMutableList()
        if (listLoaded.find { it.nameBudget == item.nameBudget } != null) {
            return false
        }
        // Load the month budget list and update the corresponding month's budget item list
        storeToMonthBudgetList(
            loadMonthBudgetList().toMutableList().also { monthBudgets ->
                monthBudgets.find { it.nameMonth == currentMonth }?.apply {
                    // Update the budgteItemList with the new item
                    val updatedList = budgteItemList.toMutableList().apply {
                        add(item)
                    }
                    budgteItemList = updatedList
                    calculateSum()
                }
            }
        )
        return true
    }

    fun saveSpecificItem(item: BudgetModel, newItem: BudgetSpecificItem, currentMonth: String) {
        storeToMonthBudgetList(
            loadMonthBudgetList().toMutableList().also { monthBudgets ->
                monthBudgets.find { it.nameMonth == currentMonth }?.apply {
                    this.budgteItemList.find { it.nameBudget == item.nameBudget }?.apply {
                        specificItemList.add(newItem)
                        calculateSum()
                    }?.let {
                        budgteItemList.toMutableList().add(it)
                    }
                    calculateSum()
                }
            }
        )
    }

    fun removeSpecificItem(item: BudgetModel, oldItemToRemove: BudgetSpecificItem, currentMonth: String) {
        storeToMonthBudgetList(
            loadMonthBudgetList().toMutableList().also { monthBudgets ->
                monthBudgets.find { it.nameMonth == currentMonth }?.apply {
                    this.budgteItemList.find { it.nameBudget == item.nameBudget }?.apply {
                        specificItemList.remove(oldItemToRemove)
                        calculateSum()
                    }?.let {
                        budgteItemList.toMutableList().add(it)
                    }
                    calculateSum()
                }
            }
        )
    }

    fun saveMonthBudget(item: MonthBudget) {
        storeToMonthBudgetList(loadedList = loadMonthBudgetList().toMutableList().also {
            it.add(item)
        })
    }

    fun saveEarningState(state: EarningModelUiState) {
    }

    fun loadMonthWithdrawList(): MutableList<MonthWithdrawModel> {
        return sharedPreferences.getString(MONTH_LIST, null)?.let { json ->
            val type = object : TypeToken<MutableList<MonthWithdrawModel>>() {}.type
            gson.fromJson(json, type)
        } ?: stubList
    }

    fun loadBudgetItemList(monthName: String): List<BudgetModel> {
        return loadMonthBudgetList().firstOrNull {
            it.nameMonth == monthName
        }?.budgteItemList ?: stubBudgetModelLists
    }

    fun loadMonthBudgetList(): List<MonthBudget> {
        return sharedPreferences.getString(MONTH_BUDGET_LIST, null)?.let { json ->
            val type = object : TypeToken<List<MonthBudget>>() {}.type
            gson.fromJson(json, type)
        } ?: stubMonthBudgetList
    }

    fun sortBudgetItemList(currentMonth: String) {
        storeToMonthBudgetList(
            loadMonthBudgetList().toMutableList().also { monthBudgets ->
                monthBudgets.find { it.nameMonth == currentMonth }?.apply {
                    val updatedList = budgteItemList.toMutableList().apply {
                        sortBy { it.levelOfRisk }
                    }
                    budgteItemList = updatedList
                }
            }
        )
    }

    fun loadUiEarningState(): EarningModelUiState {
        return sharedPreferences.getString(UISTATE, null)?.let { json ->
            val type = object : TypeToken<EarningModelUiState>() {}.type
            gson.fromJson(json, type)
        } ?: stubUiEarningState
    }

    private fun storeToMonthList(loadedList: List<MonthWithdrawModel>) {
        val json = gson.toJson(loadedList)
        sharedPreferences.edit().putString(MONTH_LIST, json).apply()
    }

    fun storeUiEarning(uiState: EarningModelUiState) {
        val json = gson.toJson(uiState)
        sharedPreferences.edit().putString(UISTATE, json).apply()
    }

    private fun storeToMonthBudgetList(loadedList: List<MonthBudget>) {
        val json = gson.toJson(loadedList)
        sharedPreferences.edit().putString(MONTH_BUDGET_LIST, json).apply()
    }

    private val stubMonthBudgetList = mutableListOf(
        MonthBudget(stubBudgetModelLists, "Jul 2024", 0, 0)
    )

    private val stubUiEarningState = EarningModelUiState()
}

val stubList = mutableListOf(
    MonthWithdrawModel("Feb 2024", mutableListOf(999f, 903f), mutableListOf("Swica", "Viaggio in Calabria")),
    MonthWithdrawModel("Jan 2024", mutableListOf(130f, 999f), mutableListOf("Insurance", "Viaggio a Monaco")),
    MonthWithdrawModel("Dec 2023", mutableListOf(999f, 800f, 1000f), mutableListOf("Swica", "Viaggio in Calabria")),
    MonthWithdrawModel("Nov 2023", mutableListOf(999f, 800f, 1000f), mutableListOf("Swica", "Viaggio in Calabria"))
)

val stubBudgetModelLists = listOf(
    BudgetModel(
        nameBudget = "Cash",
        toBeTaxed = false,
        levelOfRisk = 0.1f,
        specificItemList = mutableListOf(
            BudgetSpecificItem(nameItem = "Ubs", moneyAdd = 24000),
            BudgetSpecificItem(nameItem = "Unicredit", moneyAdd = 4000)
        ), moneyAdded = 9493, actualValue = null
    ),
    BudgetModel(
        nameBudget = "Third Pillar",
        moneyAdded = 15000,
        toBeTaxed = true,
        levelOfRisk = 0.5f,
        specificItemList = mutableListOf()
    ),
    BudgetModel(
        nameBudget = "Crypto",
        toBeTaxed = false,
        levelOfRisk = 0.9f,
        specificItemList = mutableListOf(
            BudgetSpecificItem(nameItem = "Coinbase", moneyAdd = 4000),
            BudgetSpecificItem(nameItem = "Tangem", moneyAdd = 2400),
            BudgetSpecificItem(nameItem = "Bynance", moneyAdd = 3000)
        )
    )
)
