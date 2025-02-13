/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.budget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ch.zu.chrimametro.R
import ch.zu.chrimametro.Utils.calculateThePercentage
import ch.zu.chrimametro.tools.CircleWithImage
import ch.zu.chrimametro.ui.getBackGroundColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BudgetItemCard(
    budgetModel: BudgetModel,
    onAddBudgetSpecificItem: (BudgetModel) -> Unit,
    onEditBudgetModel: (BudgetModel) -> Unit,
) {

    val percentageValue = calculateThePercentage(
        budgetModel.actualValue?.toDouble() ?: 0.0,
        budgetModel.moneyAdded.toDouble()
    )
    val imageVector =
        if (percentageValue.contains("-")) Icons.Default.KeyboardArrowDown
        else if (percentageValue.isEmpty()) null
        else Icons.Default.KeyboardArrowUp
    val colorArrow =
        if (percentageValue.contains("-")) MaterialTheme.colorScheme.error else Color(0xFF8BC34A)

    Card(
        modifier = Modifier.padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .combinedClickable(
                    onClick = { },
                    onDoubleClick = { onAddBudgetSpecificItem(budgetModel) },
                    onLongClick = { onEditBudgetModel(budgetModel) }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val temp = budgetModel.type?.imgResource ?: R.drawable.ic_various
            CircleWithImage(
                image = painterResource(id = temp),
                backgroundColor = getBackGroundColor(budgetModel.levelOfRisk)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = budgetModel.nameBudget,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val currencyFlag = if (budgetModel.currencyChf) R.drawable.ic_swiss_flag else R.drawable.ic_euro
                    if (budgetModel.toBeTaxed)
                        Image(
                            painter = painterResource(id = R.drawable.ic_tax), // replace with your actual drawable resource
                            contentDescription = "Icon",
                            modifier = Modifier.size(16.dp)
                        )
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(
                        painter = painterResource(id = currencyFlag), // replace with your actual drawable resource
                        contentDescription = "Icon",
                        modifier = Modifier.size(24.dp)
                    )
                }
                budgetModel.actualValue?.let {
                    Text(
                        text = "${(budgetModel.actualValue)} €",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (percentageValue.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        imageVector?.let {
                            Icon(imageVector = it, contentDescription = "up", tint = colorArrow)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${percentageValue}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorArrow
                        )
                    }
                }
                budgetModel.specificItemList.forEach { specificItem ->
                    Text(
                        text = "${specificItem.nameItem}: ${(specificItem.moneyAdd)} €",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}


