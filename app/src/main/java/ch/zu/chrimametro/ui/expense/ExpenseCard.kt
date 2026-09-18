/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
 */
package ch.zu.chrimametro.ui.expense

import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ch.zu.chrimametro.R
import ch.zu.chrimametro.ui.getCashFlowEmoji
import ch.zu.chrimametro.ui.getWeatherColor
import ch.zu.chrimametro.ui.getWeatherGradient
import ch.zu.chrimametro.ui.theme.OnBrand
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyCard(
   model: MonthWithdrawModel,
   viewModel: MainViewmodel?,
   initiallyExpanded: Boolean = false,
   hero: Boolean = false
) {
   val showAddExpenseDialog = remember { mutableStateOf(false) }
   val showAddNoteDialog = remember { mutableStateOf(false) }
   val showDeleteMonthDialog = remember { mutableStateOf(false) }
   val showEditFinancesDialog = remember { mutableStateOf(false) }
   val expanded = remember { mutableStateOf(initiallyExpanded) }
   val expenseText = remember { mutableStateOf("") }
   val noteText = remember { mutableStateOf("") }
   val salaryText = remember { mutableStateOf(model.salary.toString()) }
   val fixedCostsText = remember { mutableStateOf(model.fixedCosts.toString()) }
   val parsedExpense = expenseText.value.replace(",", ".").toFloatOrNull()
   val canSaveExpense = parsedExpense != null && parsedExpense > 0f
   val canSaveNote = noteText.value.trim().isNotEmpty()
   val net = model.getNet()
   val netColor = if (net >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
   val percent = model.getPercentageCashFlow()
   val weatherEmoji = getCashFlowEmoji(percent)
   val weatherColor = getWeatherColor(percent)
   val (heroGradientStart, heroGradientEnd) = getWeatherGradient(percent)
   // For dark hero gradients (storm/rain) keep text white; for light ones (sun) use dark text.
   val heroTextColor = if (percent >= 25) Color(0xFF1A1A2E) else OnBrand

   Card(
       modifier = Modifier
           .fillMaxWidth()
           .animateContentSize(),
       shape = MaterialTheme.shapes.large,
       colors = CardDefaults.cardColors(
           containerColor = if (hero) {
               MaterialTheme.colorScheme.surface
           } else {
               weatherColor.copy(alpha = 0.15f)
           },
           contentColor = MaterialTheme.colorScheme.onSurface
       )
   ) {
       Column {
           val headerModifier = if (hero) {
               Modifier
                   .fillMaxWidth()
                   .background(
                       brush = Brush.linearGradient(colors = listOf(heroGradientStart, heroGradientEnd))
                   )
           } else {
               Modifier.fillMaxWidth()
           }
           Column(modifier = headerModifier) {
               if (hero) {
                   Text(
                       text = "Current month",
                       style = MaterialTheme.typography.labelMedium,
                       fontWeight = FontWeight.Medium,
                       color = heroTextColor.copy(alpha = 0.75f),
                       modifier = Modifier.padding(start = 14.dp, top = 12.dp)
                   )
               }
               Row(
                   verticalAlignment = Alignment.CenterVertically,
                   modifier = Modifier
                       .fillMaxWidth()
                       .clickable { expanded.value = !expanded.value }
                       .padding(horizontal = 12.dp, vertical = 10.dp)
               ) {
                   Text(
                       text = weatherEmoji,
                       style = MaterialTheme.typography.headlineSmall,
                       modifier = Modifier.padding(end = 10.dp)
                   )
                   Column(modifier = Modifier.weight(1f)) {
                       Text(
                           text = model.name,
                           style = if (hero) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                           fontWeight = FontWeight.SemiBold,
                           color = if (hero) heroTextColor else MaterialTheme.colorScheme.primary
                       )
                       Text(
                           text = stringResource(R.string.month_net_value, net.toCurrencyNoDecimals()),
                           style = MaterialTheme.typography.labelMedium,
                           color = if (hero) heroTextColor.copy(alpha = 0.8f) else netColor
                       )
                   }
                   Text(
                       text = stringResource(R.string.month_total_value, model.getTotal().toCurrencyNoDecimals()),
                       style = if (hero) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                       fontWeight = FontWeight.Bold,
                       color = if (hero) heroTextColor else MaterialTheme.colorScheme.onSurface
                   )
                   Icon(
                       imageVector = if (expanded.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                       contentDescription = stringResource(
                           if (expanded.value) R.string.cd_collapse_month else R.string.cd_expand_month
                       ),
                       tint = if (hero) heroTextColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                       modifier = Modifier
                           .padding(start = 8.dp)
                           .size(24.dp)
                   )
               }
           }

           if (expanded.value) {
               Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                   Row(
                       verticalAlignment = Alignment.CenterVertically,
                       modifier = Modifier.fillMaxWidth()
                   ) {
                       Text(
                           text = stringResource(R.string.salary_value, model.salary.toCurrencyNoDecimals()),
                           style = MaterialTheme.typography.bodySmall,
                           color = MaterialTheme.colorScheme.onSurfaceVariant,
                           modifier = Modifier.weight(1f)
                       )
                       Text(
                           text = stringResource(R.string.fixed_costs_value, model.fixedCosts.toCurrencyNoDecimals()),
                           style = MaterialTheme.typography.bodySmall,
                           color = MaterialTheme.colorScheme.onSurfaceVariant
                       )
                       IconButton(onClick = {
                           salaryText.value = model.salary.toString()
                           fixedCostsText.value = model.fixedCosts.toString()
                           showEditFinancesDialog.value = true
                       }) {
                           Icon(
                               imageVector = Icons.Default.Edit,
                               contentDescription = stringResource(R.string.salary_label),
                               tint = MaterialTheme.colorScheme.primary,
                               modifier = Modifier.size(18.dp)
                           )
                       }
                       IconButton(onClick = { showDeleteMonthDialog.value = true }) {
                           Icon(
                               imageVector = Icons.Default.Delete,
                               contentDescription = stringResource(R.string.cd_delete_month),
                               tint = MaterialTheme.colorScheme.error,
                               modifier = Modifier.size(18.dp)
                           )
                       }
                   }
                   HorizontalDivider(
                       color = MaterialTheme.colorScheme.outlineVariant,
                       modifier = Modifier.padding(vertical = 8.dp)
                   )

                   if (model.expenses.isEmpty() && model.listNote.isEmpty()) {
                       Text(
                           text = stringResource(R.string.expenses_empty),
                           style = MaterialTheme.typography.bodySmall,
                           color = MaterialTheme.colorScheme.onSurfaceVariant,
                           modifier = Modifier.padding(vertical = 4.dp)
                       )
                   } else {
                       model.expenses.forEach { expense ->
                           Row(
                               verticalAlignment = Alignment.CenterVertically,
                               modifier = Modifier.fillMaxWidth()
                           ) {
                               Text(
                                   text = "· ${expense.toCurrency()} ₣",
                                   style = MaterialTheme.typography.bodyMedium,
                                   modifier = Modifier
                                       .padding(vertical = 1.dp)
                                       .weight(1f)
                               )
                               Icon(
                                   imageVector = Icons.Default.Delete,
                                   contentDescription = stringResource(R.string.cd_delete_expense),
                                   tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                   modifier = Modifier
                                       .size(16.dp)
                                       .clickable { viewModel?.deleteEntry(model.name, expense) }
                               )
                           }
                       }
                       model.listNote.forEach { note ->
                           Row(
                               verticalAlignment = Alignment.CenterVertically,
                               modifier = Modifier.fillMaxWidth()
                           ) {
                               Text(
                                   text = "📝 $note",
                                   style = MaterialTheme.typography.bodySmall,
                                   color = MaterialTheme.colorScheme.onSurfaceVariant,
                                   modifier = Modifier
                                       .padding(vertical = 1.dp)
                                       .weight(1f)
                               )
                               Icon(
                                   imageVector = Icons.Default.Delete,
                                   contentDescription = stringResource(R.string.cd_delete_note),
                                   tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                   modifier = Modifier
                                       .size(16.dp)
                                       .clickable { viewModel?.deleteNote(model.name, note) }
                               )
                           }
                       }
                   }

                   Spacer(modifier = Modifier.height(8.dp))
                   FlowRow(
                       horizontalArrangement = Arrangement.spacedBy(8.dp),
                       verticalArrangement = Arrangement.spacedBy(8.dp)
                   ) {
                       FilledTonalButton(onClick = { showAddExpenseDialog.value = true }) {
                           Icon(
                               imageVector = Icons.Default.Edit,
                               contentDescription = null,
                               modifier = Modifier.size(16.dp)
                           )
                           Spacer(modifier = Modifier.size(4.dp))
                           Text(
                               text = stringResource(R.string.add_expense),
                               style = MaterialTheme.typography.labelMedium
                           )
                       }
                       OutlinedButton(onClick = { showAddNoteDialog.value = true }) {
                           Icon(
                               painter = painterResource(id = R.drawable.ic_notes),
                               contentDescription = null,
                               modifier = Modifier.size(16.dp)
                           )
                           Spacer(modifier = Modifier.size(4.dp))
                           Text(
                               text = stringResource(R.string.add_note),
                               style = MaterialTheme.typography.labelMedium
                           )
                       }
                   }
                   Spacer(modifier = Modifier.height(8.dp))
               }
           }
       }
   }
    
   if (showAddExpenseDialog.value && viewModel != null) {
       AlertDialog(
           onDismissRequest = { showAddExpenseDialog.value = false },
           title = { Text(text = stringResource(R.string.new_expense_for_month, model.name)) },
           text = {
               OutlinedTextField(
                   value = expenseText.value,
                   onValueChange = { expenseText.value = it },
                   singleLine = true,
                   label = { Text(stringResource(R.string.expense_amount_label)) },
                   keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
               )
           },
           confirmButton = {
               TextButton(
                   onClick = {
                       parsedExpense?.let { viewModel.storeInput(model.name, it) }
                       expenseText.value = ""
                       showAddExpenseDialog.value = false
                   },
                   enabled = canSaveExpense
               ) {
                   Text(stringResource(R.string.action_save))
               }
           },
           dismissButton = {
               TextButton(onClick = {
                   showAddExpenseDialog.value = false
                   expenseText.value = ""
               }) {
                   Text(stringResource(R.string.action_cancel))
               }
           }
       )
   }

   if (showAddNoteDialog.value && viewModel != null) {
       AlertDialog(
           onDismissRequest = { showAddNoteDialog.value = false },
           title = { Text(text = stringResource(R.string.new_note_for_month, model.name)) },
           text = {
               OutlinedTextField(
                   value = noteText.value,
                   onValueChange = { noteText.value = it },
                   label = { Text(stringResource(R.string.note_label)) }
               )
           },
           confirmButton = {
               TextButton(
                   onClick = {
                       viewModel.addOrUpdateNoteForMonth(model.name, noteText.value.trim())
                       noteText.value = ""
                       showAddNoteDialog.value = false
                   },
                   enabled = canSaveNote
               ) {
                   Text(stringResource(R.string.action_save))
               }
           },
           dismissButton = {
               TextButton(onClick = {
                   showAddNoteDialog.value = false
                   noteText.value = ""
               }) {
                   Text(stringResource(R.string.action_cancel))
               }
           }
       )
   }

   if (showDeleteMonthDialog.value) {
       AlertDialog(
           onDismissRequest = { showDeleteMonthDialog.value = false },
           title = { Text(text = stringResource(R.string.delete_month_title, model.name)) },
           text = {
               Text(stringResource(R.string.delete_month_message))
           },
           confirmButton = {
               TextButton(
                   onClick = {
                       viewModel?.deleteMonth(model)
                       showDeleteMonthDialog.value = false
                   },
                   colors = ButtonDefaults.textButtonColors(
                       contentColor = MaterialTheme.colorScheme.error
                   )
               ) {
                   Text(stringResource(R.string.action_delete))
               }
           },
           dismissButton = {
               TextButton(onClick = { showDeleteMonthDialog.value = false }) {
                   Text(stringResource(R.string.action_cancel))
               }
           }
       )
   }

   if (showEditFinancesDialog.value && viewModel != null) {
       val parsedSalary = salaryText.value.replace(",", ".").toFloatOrNull()
       val parsedFixedCosts = fixedCostsText.value.replace(",", ".").toFloatOrNull()
       val canSaveFinances = parsedSalary != null && parsedSalary > 0f &&
               parsedFixedCosts != null && parsedFixedCosts >= 0f

       AlertDialog(
           onDismissRequest = { showEditFinancesDialog.value = false },
           title = { Text(text = stringResource(R.string.edit_month_finances_title, model.name)) },
           text = {
               Column {
                   OutlinedTextField(
                       value = salaryText.value,
                       onValueChange = { salaryText.value = it },
                       singleLine = true,
                       label = { Text(stringResource(R.string.salary_input_label)) },
                       keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                   )
                   Spacer(modifier = Modifier.height(8.dp))
                   OutlinedTextField(
                       value = fixedCostsText.value,
                       onValueChange = { fixedCostsText.value = it },
                       singleLine = true,
                       label = { Text(stringResource(R.string.fixed_costs_input_label)) },
                       keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                   )
               }
           },
           confirmButton = {
               TextButton(
                   onClick = {
                       if (parsedSalary != null && parsedFixedCosts != null) {
                           viewModel.updateMonthFinances(model.name, parsedSalary, parsedFixedCosts)
                       }
                       showEditFinancesDialog.value = false
                   },
                   enabled = canSaveFinances
               ) {
                   Text(stringResource(R.string.action_save))
               }
           },
           dismissButton = {
               TextButton(onClick = { showEditFinancesDialog.value = false }) {
                   Text(stringResource(R.string.action_cancel))
               }
           }
       )
   }
}

private fun Float.toCurrency(): String = String.format(Locale.getDefault(), "%.2f", this)

private fun Double.toCurrency(): String = String.format(Locale.getDefault(), "%.2f", this)

private fun Double.toCurrencyNoDecimals(): String = String.format(Locale.getDefault(), "%.0f", this)

private fun Float.toCurrencyNoDecimals(): String = this.toDouble().toCurrencyNoDecimals()
