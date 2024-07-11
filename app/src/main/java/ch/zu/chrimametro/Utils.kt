/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro

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
}
