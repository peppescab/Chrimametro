/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro

import android.content.Context
import ch.zu.chrimametro.ui.budget.BudgetModel
import ch.zu.chrimametro.ui.budget.BudgetSpecificItem
import ch.zu.chrimametro.ui.earning.EarningModelUiState
import ch.zu.chrimametro.ui.expense.MonthWithdrawModel
import ch.zu.chrimametro.ui.monthbudget.MonthBudget
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferenceManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext context: Context
) {
    private val gson = Gson()
    private val appStateDoc = firestore.collection("chrimametro").document("app_state")
    private val legacyPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    private val MONTH_LIST = "monthWithdrawList"
    private val MONTH_BUDGET_LIST = "monthBudgetItemL"
    private val UISTATE = "uiEarningState"

    suspend fun removeMonthWithdrawList(monthName: String, amountToRemove: Float? = null, noteToRemove: String? = null) {
        val loadedList = loadMonthWithdrawList()
        val monthWithdrawModel = loadedList.find { it.name == monthName }
        if (amountToRemove == null) {
            monthWithdrawModel?.listNote?.remove(noteToRemove)
        } else {
            monthWithdrawModel?.expenses?.remove(amountToRemove)
        }
        storeToMonthList(loadedList)
    }

    suspend fun removeMonth(month: MonthWithdrawModel) {
        storeToMonthList(loadMonthWithdrawList().also {
            it.remove(month)
        })
    }

    suspend fun updateMonthFinances(monthName: String, salary: Float, fixedCosts: Float) {
        val loadedList = loadMonthWithdrawList()
        val index = loadedList.indexOfFirst { it.name == monthName }
        if (index >= 0) {
            val old = loadedList[index]
            loadedList[index] = old.copy(
                salary = salary,
                fixedCosts = fixedCosts,
                houseCost = fixedCosts,
                insuranceCost = 0f
            )
            storeToMonthList(loadedList)
        }
    }

    suspend fun updateMonthFinancialDetails(
        monthName: String,
        salary: Float,
        houseCost: Float,
        insuranceCost: Float
    ) {
        val loadedList = loadMonthWithdrawList()
        val index = loadedList.indexOfFirst { it.name == monthName }
        if (index >= 0) {
            val old = loadedList[index]
            loadedList[index] = old.copy(
                salary = salary,
                fixedCosts = houseCost + insuranceCost,
                houseCost = houseCost,
                insuranceCost = insuranceCost
            )
            storeToMonthList(loadedList)
        }
    }

    suspend fun removeMonthBudget(month: MonthBudget) {
        storeToMonthBudgetList(loadMonthBudgetList().toMutableList().also {
            it.remove(month)
        })
    }

    suspend fun removeBudgetItemList(item: BudgetModel, currentMonth: String) {
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

    suspend fun saveMonthWithdrawList(
        monthName: String, valueToAdd: Float? = null, noteToAdd: String? = null
    ) {
        val loadedList = loadMonthWithdrawList().toMutableList()
        val monthWithdrawModel = loadedList.find { it.name == monthName }
        valueToAdd?.let {
            monthWithdrawModel?.expenses?.add(it)
        } ?: run {
            if (noteToAdd == null) {
                val earningState = loadUiEarningState()
                val houseCost = earningState.houseCost.toFloatOrNull() ?: 0f
                val insuranceCost = earningState.insuranceCost.toFloatOrNull() ?: 0f
                val totalExpenses = insuranceCost + houseCost

                loadedList.add(
                    0, MonthWithdrawModel(
                        monthName,
                        mutableListOf(),
                        salary = earningState.netMonthlySalary.toFloat(),
                        fixedCosts = totalExpenses,
                        houseCost = houseCost,
                        insuranceCost = insuranceCost
                    )
                )
            } else {
                monthWithdrawModel?.listNote?.add(noteToAdd)
            }
        }
        storeToMonthList(loadedList)
    }

    suspend fun saveBudgetItemList(item: BudgetModel, currentMonth: String): Boolean {
        val listLoaded = loadBudgetItemList(currentMonth).toMutableList()
        if (listLoaded.find { it.nameBudget == item.nameBudget } != null) {
            return false
        }
        storeToMonthBudgetList(
            loadMonthBudgetList().toMutableList().also { monthBudgets ->
                monthBudgets.find { it.nameMonth == currentMonth }?.apply {
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

    suspend fun saveSpecificItem(item: BudgetModel, newItem: BudgetSpecificItem, currentMonth: String) {
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

    suspend fun removeSpecificItem(item: BudgetModel, oldItemToRemove: BudgetSpecificItem, currentMonth: String) {
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

    suspend fun saveMonthBudget(item: MonthBudget) {
        storeToMonthBudgetList(loadedList = loadMonthBudgetList().toMutableList().also {
            it.add(item)
        })
    }

    suspend fun saveEarningState(state: EarningModelUiState) {
        storeUiEarning(state)
    }

    suspend fun loadMonthWithdrawList(): MutableList<MonthWithdrawModel> {
        val remoteJson = getStateJson(MONTH_LIST)
        val localJson = legacyPreferences.getString(MONTH_LIST, null)

        val remoteList = remoteJson?.let { parseMonthWithdrawList(it) }
        val localList = localJson?.let { parseMonthWithdrawList(it) }

        val resolvedList = when {
            localList != null && remoteList == null -> localList
            localList != null && remoteList != null && shouldPreferLocalMonths(localList, remoteList) -> localList
            remoteList != null -> remoteList
            localList != null -> localList
            else -> stubList.toMutableList()
        }

        val normalizedList = resolvedList.map { month ->
            val normalizedHouse = if (month.houseCost == 0f && month.insuranceCost == 0f && month.fixedCosts > 0f) {
                month.fixedCosts
            } else {
                month.houseCost
            }
            val normalizedInsurance = if (month.houseCost == 0f && month.insuranceCost == 0f && month.fixedCosts > 0f) {
                0f
            } else {
                month.insuranceCost
            }
            month.copy(
                houseCost = normalizedHouse,
                insuranceCost = normalizedInsurance,
                fixedCosts = normalizedHouse + normalizedInsurance
            )
        }.toMutableList()

        val resolvedJson = gson.toJson(normalizedList)
        if (remoteJson != resolvedJson) {
            storeStateJson(MONTH_LIST, resolvedJson)
        }

        return normalizedList
    }

    suspend fun loadBudgetItemList(monthName: String): List<BudgetModel> {
        return loadMonthBudgetList().firstOrNull {
            it.nameMonth == monthName
        }?.budgteItemList ?: stubBudgetModelLists
    }

    suspend fun loadMonthBudgetList(): List<MonthBudget> {
        return getStateJson(MONTH_BUDGET_LIST)?.let { json ->
            val type = object : TypeToken<List<MonthBudget>>() {}.type
            gson.fromJson<List<MonthBudget>>(json, type)
        } ?: run {
            storeToMonthBudgetList(stubMonthBudgetList)
            stubMonthBudgetList
        }
    }

    suspend fun sortBudgetItemList(currentMonth: String) {
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

    suspend fun loadUiEarningState(): EarningModelUiState {
        return getStateJson(UISTATE)?.let { json ->
            val type = object : TypeToken<EarningModelUiState>() {}.type
            gson.fromJson<EarningModelUiState>(json, type)
        } ?: run {
            storeUiEarning(stubUiEarningState)
            stubUiEarningState
        }
    }

    private suspend fun storeToMonthList(loadedList: List<MonthWithdrawModel>) {
        val json = gson.toJson(loadedList)
        storeStateJson(MONTH_LIST, json)
    }

    suspend fun storeUiEarning(uiState: EarningModelUiState) {
        val json = gson.toJson(uiState)
        storeStateJson(UISTATE, json)
    }

    private suspend fun storeToMonthBudgetList(loadedList: List<MonthBudget>) {
        val json = gson.toJson(loadedList)
        storeStateJson(MONTH_BUDGET_LIST, json)
    }

    private suspend fun getStateJson(key: String): String? {
        val snapshot = appStateDoc.get().await()
        return snapshot.getString(key)
    }

    private suspend fun storeStateJson(key: String, json: String) {
        appStateDoc.set(mapOf(key to json), SetOptions.merge()).await()
    }

    private fun parseMonthWithdrawList(json: String): MutableList<MonthWithdrawModel> {
        val type = object : TypeToken<MutableList<MonthWithdrawModel>>() {}.type
        return gson.fromJson<MutableList<MonthWithdrawModel>>(json, type)
    }

    private fun shouldPreferLocalMonths(
        localList: List<MonthWithdrawModel>,
        remoteList: List<MonthWithdrawModel>
    ): Boolean {
        val localMonthSet = localList.map { it.name }.toSet()
        val remoteMonthSet = remoteList.map { it.name }.toSet()

        if ((localMonthSet - remoteMonthSet).isNotEmpty()) {
            return true
        }

        if (localList.size > remoteList.size) {
            return true
        }

        val localScore = localList.sumOf { it.expenses.size + it.listNote.size } + localList.size
        val remoteScore = remoteList.sumOf { it.expenses.size + it.listNote.size } + remoteList.size
        return localScore > remoteScore
    }

    private val stubMonthBudgetList = mutableListOf(
        MonthBudget(stubBudgetModelLists, "Jul 2024", 0, 0)
    )

    private val stubUiEarningState = EarningModelUiState()
}

val stubList = mutableListOf(
    MonthWithdrawModel("Nov 2025", mutableListOf(999f, 847f, 999f), mutableListOf("400 marocco"), 7408f, 2650f),
    MonthWithdrawModel("Oct 2025", mutableListOf(300f, 999f, 463f), mutableListOf("tromso 200"), 7408f, 2650f),
    MonthWithdrawModel(
        "Sep 2025",
        mutableListOf(999f, 999f, 130f, 500f, 400f, 500f, 500f),
        mutableListOf("anelli 1000", "cardiologo 132", "dentista 112", "passaporto 115"),
        7408f,
        2650f
    ),
    MonthWithdrawModel(
        "Aug 2025",
        mutableListOf(999f, 150f, 463f, 400f, 300f),
        mutableListOf("ombrellone 400"),
        7408f,
        2650f
    ),
    MonthWithdrawModel("Jul 2025", mutableListOf(999f, 999f, 460f), mutableListOf(), 7408f, 2650f),
    MonthWithdrawModel(
        "Jun 2025",
        mutableListOf(999f, 999f, 999f, 999f, 663f, 485f),
        mutableListOf("televisione 605", "trasloco 600", "Ikea 240", "viaggio a bari 800", "Amazon 200"),
        7408f,
        2650f
    ),
    MonthWithdrawModel(
        "May 2025",
        mutableListOf(500f, 800f, 1000f, 534f, 333f),
        mutableListOf("stellato 175", "Ikea 2300"),
        7398f,
        2269f
    ),
    MonthWithdrawModel("Apr 2025", mutableListOf(666f, 999f, 300f), mutableListOf(), 7398f, 2269f),
    MonthWithdrawModel("Mar 2025", mutableListOf(999f, 333f, 333f, 333f), mutableListOf(), 7369f, 2269f),
    MonthWithdrawModel("Feb 2025", mutableListOf(999f, 999f, 999f, 999f), mutableListOf(), 7142f, 2269f),
    MonthWithdrawModel("Jan 2025", mutableListOf(999f, 999f), mutableListOf(), 7142f, 2269f),
    MonthWithdrawModel("Dec 2024", mutableListOf(999f, 999f, 999f), mutableListOf(), 7142f, 2269f),
    MonthWithdrawModel("Nov 2024", mutableListOf(999f, 665f), mutableListOf(), 7142f, 2269f),
    MonthWithdrawModel("Oct 2024", mutableListOf(999f, 999f), mutableListOf(), 7142f, 2269f),
    MonthWithdrawModel("Sep 2024", mutableListOf(999f, 333f, 200f), mutableListOf(), 7142f, 2269f),
    MonthWithdrawModel("Aug 2024", mutableListOf(999f, 999f, 999f, 999f), mutableListOf(), 7142f, 2269f),
    MonthWithdrawModel("Jul 2024", mutableListOf(999f, 999f), mutableListOf(), 7142f, 2269f),
    MonthWithdrawModel("Jun 2024", mutableListOf(999f, 999f, 999f), mutableListOf(), 7142f, 2269f),
    MonthWithdrawModel("May 2024", mutableListOf(999f, 999f, 999f), mutableListOf(), 7142f, 2269f),
    MonthWithdrawModel("Apr 2024", mutableListOf(999f, 999f, 500f, 333f), mutableListOf(), 7142f, 2269f),
    MonthWithdrawModel("Mar 2024", mutableListOf(1000f, 400f), mutableListOf(), 7142f, 2269f),
    MonthWithdrawModel(
        "Feb 2024",
        mutableListOf(999f, 340f, 999f, 903f),
        mutableListOf("Swica", "Viaggio in Calabria"),
        7142f,
        2269f
    ),
    MonthWithdrawModel(
        "Jan 2024",
        mutableListOf(130f, 999f),
        mutableListOf("Insurance", "Viaggio a Monaco"),
        7142f,
        2269f
    ),
    MonthWithdrawModel(
        "Dec 2023",
        mutableListOf(999f, 800f, 1000f),
        mutableListOf("Swica", "Viaggio in Calabria"),
        7142f,
        2269f
    ),
    MonthWithdrawModel(
        "Nov 2023",
        mutableListOf(999f, 800f, 1000f),
        mutableListOf("Swica", "Viaggio in Calabria"),
        7142f,
        2269f
    )

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
