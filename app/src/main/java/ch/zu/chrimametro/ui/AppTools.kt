/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui

import androidx.compose.ui.graphics.Color

fun getBackGroundColor(riskLevel: Float): Color = when {
    riskLevel == 0.0f -> Color(0xFF4FC3F7)
    riskLevel <= 0.17f -> Color(0xFF4CAF50)
    riskLevel <= 0.34f -> Color(0xFF8BC34A)
    riskLevel <= 0.50f -> Color(0xFFFFEB3B)
    riskLevel <= 0.67f -> Color(0xFFFFC107)
    riskLevel <= 0.84f -> Color(0xFFFF5722)
    riskLevel <= 1f -> Color(0xFFE91E63)
    else -> Color.Gray // Invalid risk level, handle accordingly
}

fun getExpensesBackground(money: Double): Color = when {
    money <= 2000.0f -> Color(0xFF80CBC4)
    money <= 2500.0f -> Color(0xFFA5D6A7)
    money <= 3000.0f -> Color(0xFFFFCA28)
    money <= 3500.0f -> Color(0xFFFFA726)
    money <= 4000.0f -> Color(0xFF7E57C2)
    else -> Color(0xFFEF5350)
}
