/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui

import androidx.compose.ui.graphics.Color

val years = listOf("2026", "2025", "2024", "2023")
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
    money <= 2500.0 -> Color(0xFF00796B) // Green 500
    money <= 3000.0 -> Color(0xFF689F38) // Yellow 500
    money <= 3500.0 -> Color(0xFFFBC02D) // Orange 500
    money <= 4000.0 -> Color(0xFFFFA000) // Orange 500
    money <= 4500.0 -> Color(0xFFF57C00) // Orange 500
    else -> Color(0xFFE64A19)           // Red 500
}

fun getCashFlowBackground(money: Double): Color = when {
    money >= 3000.0 -> Color(0xFF4CAF50) // green – very good
    money >= 2500.0 -> Color(0xFF8BC34A) // light green
    money >= 2000.0 -> Color(0xFFFFEB3B) // yellow
    money >= 1500.0 -> Color(0xFFFF9800) // orange-yellow
    money >= 1000.0 -> Color(0xFFFF5722) // orange
    else -> Color(0xFFE91E63) // red – low cash flow
}

fun getCashFlowEmoji(percent: Float): String = when {
    percent >= 40 -> "☀️"   // eccellente
    percent >= 25 -> "🌤️"  // buono
    percent >= 10 -> "🌧️"   // negativo
    else -> "🌪️"           // molto negativo
}

fun fromEmojiToColor(emoji: String): Color = when (emoji) {
    "☀️" -> Color(0xFF4CAF50)
    "🌤️" -> Color(0xFFFFEB3B)
    "🌧️" -> Color(0xFFFF5722)
    else -> Color(0xFF673AB7)
}


