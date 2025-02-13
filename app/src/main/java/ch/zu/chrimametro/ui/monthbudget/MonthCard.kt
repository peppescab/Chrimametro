/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.monthbudget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthBudgetCard(
    monthModel: MonthBudget,
    onClickItem: (MonthBudget) -> Unit,
    onDoubleClickItem: (MonthBudget) -> Unit
) {

    val collapseCard = remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .combinedClickable(
                onClick = { onClickItem(monthModel) },
                onDoubleClick = { onDoubleClickItem(monthModel) }
            ),

        colors = CardDefaults.cardColors(
            containerColor = getBackGroundColor(monthModel.nameMonth),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthModel.nameMonth,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Icon",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            collapseCard.value = !collapseCard.value
                        }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Current value: ${(monthModel.currentAmount)} €",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total Added: ${(monthModel.totalAmount)} €",
                style = MaterialTheme.typography.bodyMedium
            )
            if (collapseCard.value.not()) {
                Spacer(modifier = Modifier.height(8.dp))
                monthModel.budgteItemList.forEach { specificItem ->
                    Text(
                        text = "${specificItem.nameBudget}: ${specificItem.actualValue} €",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun getBackGroundColor(monthName: String): Color = when {
    monthName.contains("Jan") -> Color(0xFFFFCDD2)
    monthName.contains("Feb") -> Color(0xFFF8BBD0)
    monthName.contains("Mar") -> Color(0xFFE1BEE7)
    monthName.contains("Apr") -> Color(0xFFD1C4E9)
    monthName.contains("May") -> Color(0xFFC5CAE9)
    monthName.contains("Jun") -> Color(0xFFBBDEFB)
    monthName.contains("Jul") -> Color(0xFFB3E5FC)
    monthName.contains("Aug") -> Color(0xFFB2EBF2)
    monthName.contains("Sep") -> Color(0xFFFFCC80)
    monthName.contains("Oct") -> Color(0xFF90CAF9)
    monthName.contains("Nov") -> Color(0xFFB39DDB)
    monthName.contains("Dec") -> Color(0xFF80CBC4)
    else -> Color.Gray // Invalid risk level, handle accordingly
}
