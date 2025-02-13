/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.earning

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.zu.chrimametro.R
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun EarningsScreen(
    viewModel: EarningsViewModel
) {

    val uiState by viewModel.uiState.collectAsState()

    val netSalary = remember { mutableStateOf(uiState.netMonthlySalary) }
    val houseCost = remember { mutableStateOf(uiState.houseCost) }
    val insuranceCost = remember { mutableStateOf(uiState.insuranceCost) }

    var earningsPerSecond by remember { mutableDoubleStateOf(0.0) }
    var totalEarned by remember { mutableDoubleStateOf(0.0) }
    var progress by remember { mutableFloatStateOf(0f) }
    val isWorkingHours = remember { mutableStateOf(false) }

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
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val colorProgress = MaterialTheme.colorScheme.primary
        val colorTrack = MaterialTheme.colorScheme.onSurfaceVariant
        if (isWorkingHours.value) {
            // Circular progress indicator
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
                value = netSalary.value,
                onValueChange = {
                    netSalary.value = it
                    viewModel.saveState(uiState.copy(netMonthlySalary = it))
                },
                label = { Text("Net Monthly Salary") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Input for net monthly salary
            OutlinedTextField(
                value = houseCost.value,
                onValueChange = {
                    houseCost.value = it
                    viewModel.saveState(uiState.copy(houseCost = it))
                },
                label = { Text("House rent cost") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Spacer(modifier = Modifier.height(16.dp))
            // Input for net monthly salary
            OutlinedTextField(
                value = insuranceCost.value,
                onValueChange = {
                    insuranceCost.value = it
                    viewModel.saveState(uiState.copy(insuranceCost = it))
                },
                label = { Text("Insurance cost") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    modifier = Modifier.size(300.dp),
                    painter = painterResource(id = R.drawable.ic_spa),
                    contentDescription = "Spa"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Relax no work now!",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}
