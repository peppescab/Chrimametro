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
import androidx.core.content.edit

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
        sharedPreferences.edit { putString(MONTH_LIST, json) }
    }

    fun storeUiEarning(uiState: EarningModelUiState) {
        val json = gson.toJson(uiState)
        sharedPreferences.edit { putString(UISTATE, json) }
    }

    private fun storeToMonthBudgetList(loadedList: List<MonthBudget>) {
        val json = gson.toJson(loadedList)
        sharedPreferences.edit { putString(MONTH_BUDGET_LIST, json) }
    }

    private val stubMonthBudgetList = mutableListOf(
        MonthBudget(stubBudgetModelLists, "Jul 2024", 0, 0)
    )

    private val stubUiEarningState = EarningModelUiState()
}

val stubList = mutableListOf(
    MonthWithdrawModel("Nov 2025", mutableListOf(999f, 847f, 999f), mutableListOf("400 marocco")),
    MonthWithdrawModel("Oct 2025", mutableListOf(300f, 999f, 463f), mutableListOf("tromso 200")),
    MonthWithdrawModel("Sep 2025", mutableListOf(999f, 999f, 130f, 500f, 400f, 500f, 500f), mutableListOf("anelli 1000","cardiologo 132","dentista 112","passaporto 115")),
    MonthWithdrawModel("Aug 2025", mutableListOf(999f, 150f, 463f, 400f, 300f), mutableListOf("ombrellone 400")),
    MonthWithdrawModel("Jul 2025", mutableListOf(999f, 999f, 460f), mutableListOf()),
    MonthWithdrawModel("Jun 2025", mutableListOf(999f, 999f, 999f, 999f, 663f, 485f), mutableListOf("televisione 605","trasloco 600","Ikea 240","viaggio a bari 800","Amazon 200")),
    MonthWithdrawModel("May 2025", mutableListOf(500f, 800f, 1000f, 534f, 333f), mutableListOf("stellato 175","Ikea 2300")),
    MonthWithdrawModel("Apr 2025", mutableListOf(666f, 999f, 300f), mutableListOf()),
    MonthWithdrawModel("Mar 2025", mutableListOf(999f, 333f, 333f, 333f), mutableListOf()),
    MonthWithdrawModel("Feb 2025", mutableListOf(999f, 999f, 999f, 999f), mutableListOf()),
    MonthWithdrawModel("Jan 2025", mutableListOf(999f, 999f), mutableListOf()),
    MonthWithdrawModel("Dec 2024", mutableListOf(999f, 999f, 999f), mutableListOf()),
    MonthWithdrawModel("Nov 2024", mutableListOf(999f, 665f), mutableListOf()),
    MonthWithdrawModel("Oct 2024", mutableListOf(999f, 999f), mutableListOf()),
    MonthWithdrawModel("Sep 2024", mutableListOf(999f, 333f, 200f), mutableListOf()),
    MonthWithdrawModel("Aug 2024", mutableListOf(999f, 999f, 999f, 999f), mutableListOf()),
    MonthWithdrawModel("Jul 2024", mutableListOf(999f, 999f), mutableListOf()),
    MonthWithdrawModel("Jun 2024", mutableListOf(999f, 999f, 999f), mutableListOf()),
    MonthWithdrawModel("May 2024", mutableListOf(999f, 999f, 999f), mutableListOf()),
    MonthWithdrawModel("Apr 2024", mutableListOf(999f, 999f, 500f, 333f), mutableListOf()),
    MonthWithdrawModel("Mar 2024", mutableListOf(1000f, 400f), mutableListOf()),
    MonthWithdrawModel("Feb 2024", mutableListOf(999f, 340f, 999f, 903f), mutableListOf("Swica","Viaggio in Calabria")),
    MonthWithdrawModel("Jan 2024", mutableListOf(130f, 999f), mutableListOf("Insurance","Viaggio a Monaco")),
    MonthWithdrawModel("Dec 2023", mutableListOf(999f, 800f, 1000f), mutableListOf("Swica","Viaggio in Calabria")),
    MonthWithdrawModel("Nov 2023", mutableListOf(999f, 800f, 1000f), mutableListOf("Swica","Viaggio in Calabria"))
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
