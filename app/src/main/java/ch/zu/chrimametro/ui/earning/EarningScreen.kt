/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.earning

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.zu.chrimametro.R
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.Calendar

@Composable
fun EarningsScreen(
    viewModel: EarningsViewModel
) {

    val uiState by viewModel.uiState.collectAsState()
    val monthSalaryHistory by viewModel.monthSalaryHistory.collectAsState()

    var netSalary by remember(uiState.netMonthlySalary) { mutableStateOf(uiState.netMonthlySalary) }
    var houseCost by remember(uiState.houseCost) { mutableStateOf(uiState.houseCost) }
    var insuranceCost by remember(uiState.insuranceCost) { mutableStateOf(uiState.insuranceCost) }
    val editMonthName = remember { mutableStateOf<String?>(null) }
    val editSalaryText = remember { mutableStateOf("") }
    val editHouseText = remember { mutableStateOf("") }
    val editInsuranceText = remember { mutableStateOf("") }

    var earningsPerSecond by remember { mutableDoubleStateOf(0.0) }
    var totalEarned by remember { mutableDoubleStateOf(0.0) }
    var progress by remember { mutableFloatStateOf(0f) }
    val isWorkingHours = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshMonthSalaryHistory()
    }

    // Function to calculate earnings per second
    fun calculateEarningsPerSecond(salary: Double): Double {
        val workingDaysPerMonth = 22 // Assuming 22 working days per month
        val workingHoursPerDay = 9 // 8 AM - 5 PM
        val workingSecondsPerMonth = workingDaysPerMonth * workingHoursPerDay * 3600
        return salary / workingSecondsPerMonth
    }

    // Function to calculate the total earnings since the start of the workday
    fun calculateInitialEarnings(): Double {
        val currentTime = Calendar.getInstance()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
        }

        // Calculate seconds passed since 8 AM
        val secondsSinceStart = (currentTime.timeInMillis - startOfDay.timeInMillis) / 1000
        return if (secondsSinceStart > 0) {
            secondsSinceStart * earningsPerSecond
        } else 0.0
    }

    // Function to check if it's working hours (8:30 AM to 5:30 PM)
    fun checkWorkingHours(): Boolean {
        val currentTime = Calendar.getInstance()
        val dayOfWeek = currentTime.get(Calendar.DAY_OF_WEEK)

        // Get current hour and minute
        val hourOfDay = currentTime.get(Calendar.HOUR_OF_DAY)
        val minuteOfHour = currentTime.get(Calendar.MINUTE)

        val isWeekday = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY

        // Check if the time is within the working hours
        val isAfterStartTime = (hourOfDay > 8) || (hourOfDay == 8 && minuteOfHour >= 30)
        val isBeforeEndTime = (hourOfDay < 17) || (hourOfDay == 17 && minuteOfHour < 30)

        return isWeekday && isAfterStartTime && isBeforeEndTime
    }

    // Function to calculate the progress based on the current time
    fun calculateProgress(): Float {
        val currentTime = Calendar.getInstance()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
        }
        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
        }

        val totalSecondsInDay = (endOfDay.timeInMillis - startOfDay.timeInMillis) / 1000
        val secondsPassed = (currentTime.timeInMillis - startOfDay.timeInMillis) / 1000

        return (secondsPassed.toFloat() / totalSecondsInDay.toFloat()).coerceIn(0f, 1f)
    }

    // Recalculate earnings per second whenever the salary, house cost, or insurance cost changes
    LaunchedEffect(uiState.netMonthlySalary, uiState.houseCost, uiState.insuranceCost) {
        val salary = uiState.netMonthlySalary.toDoubleOrNull() ?: 0.0
        val house = uiState.houseCost.toDoubleOrNull() ?: 0.0
        val insurance = uiState.insuranceCost.toDoubleOrNull() ?: 0.0

        earningsPerSecond = calculateEarningsPerSecond(salary - house - insurance)
    }

    // Initialize total earned with past earnings when the component is first composed
    LaunchedEffect(earningsPerSecond) {
        if (earningsPerSecond > 0 && checkWorkingHours()) {
            totalEarned = calculateInitialEarnings()
            progress = calculateProgress()

            while (true) {
                if (checkWorkingHours()) {
                    isWorkingHours.value = true
                    delay(1000L) // 1-second delay
                    totalEarned += earningsPerSecond
                    progress = calculateProgress()
                } else {
                    isWorkingHours.value = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val colorProgress = MaterialTheme.colorScheme.primary
        val colorTrack = MaterialTheme.colorScheme.onSurfaceVariant
        // Circular progress indicator

        if(isWorkingHours.value)
            Box(
                modifier = Modifier
                    .size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(300.dp)) {
                    val sweepAngle = progress * 360f

                    // Draw background circle
                    drawCircle(
                        color = colorTrack,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 30.dp.toPx())
                    )

                    // Draw the progress arc
                    drawArc(
                        color = colorProgress,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Display the earnings inside the circle
                Text(
                    text = String.format("%.2f", totalEarned),
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            // Input for net monthly salary
            OutlinedTextField(
                value = netSalary,
                onValueChange = {
                    netSalary = it
                    viewModel.saveState(uiState.copy(netMonthlySalary = it))
                },
                label = { Text(stringResource(R.string.earning_net_salary_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Input for net monthly salary
            OutlinedTextField(
                value = houseCost,
                onValueChange = {
                    houseCost = it
                    viewModel.saveState(uiState.copy(houseCost = it))
                },
                label = { Text(stringResource(R.string.earning_house_cost_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Spacer(modifier = Modifier.height(16.dp))
            // Input for net monthly salary
            OutlinedTextField(
                value = insuranceCost,
                onValueChange = {
                    insuranceCost = it
                    viewModel.saveState(uiState.copy(insuranceCost = it))
                },
                label = { Text(stringResource(R.string.earning_insurance_cost_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.monthly_salary_trend_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            monthSalaryHistory.forEach { month ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = month.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.monthly_salary_value, month.salary.toCurrency()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.monthly_house_value, month.houseCost.toCurrency()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.monthly_insurance_value, month.insuranceCost.toCurrency()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.monthly_fixed_total_value, month.fixedCosts.toCurrency()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            editMonthName.value = month.name
                            editSalaryText.value = month.salary.toString()
                            editHouseText.value = month.houseCost.toString()
                            editInsuranceText.value = month.insuranceCost.toString()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.salary_label)
                            )
                        }
                    }
                }
            }
    }

    if (editMonthName.value != null) {
        val parsedSalary = editSalaryText.value.replace(",", ".").toFloatOrNull()
        val parsedHouseCost = editHouseText.value.replace(",", ".").toFloatOrNull()
        val parsedInsuranceCost = editInsuranceText.value.replace(",", ".").toFloatOrNull()
        val canSave = parsedSalary != null && parsedSalary > 0f &&
            parsedHouseCost != null && parsedHouseCost >= 0f &&
            parsedInsuranceCost != null && parsedInsuranceCost >= 0f
        AlertDialog(
            onDismissRequest = { editMonthName.value = null },
            title = {
                Text(text = stringResource(R.string.edit_month_finance_title, editMonthName.value ?: ""))
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = editSalaryText.value,
                        onValueChange = { editSalaryText.value = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.month_salary_input_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editHouseText.value,
                        onValueChange = { editHouseText.value = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.month_house_input_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editInsuranceText.value,
                        onValueChange = { editInsuranceText.value = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.month_insurance_input_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (parsedSalary != null && parsedHouseCost != null && parsedInsuranceCost != null) {
                            viewModel.updateMonthFinancialDetails(
                                monthName = editMonthName.value.orEmpty(),
                                salary = parsedSalary,
                                houseCost = parsedHouseCost,
                                insuranceCost = parsedInsuranceCost
                            )
                            editMonthName.value = null
                        }
                    },
                    enabled = canSave
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editMonthName.value = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

private fun Float.toCurrency(): String = String.format(Locale.getDefault(), "%.2f", this)
