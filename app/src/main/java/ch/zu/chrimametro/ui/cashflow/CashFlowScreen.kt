/*
 * Copyright © 2014-2025, TWINT AG.
 * All rights reserved.
 */
package ch.zu.chrimametro.ui.cashflow

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.zu.chrimametro.ui.expense.MainViewmodel
import ch.zu.chrimametro.ui.expense.MonthWithdrawModel
import ch.zu.chrimametro.ui.fromEmojiToColor
import ch.zu.chrimametro.ui.getCashFlowBackground
import ch.zu.chrimametro.ui.getCashFlowEmoji
import ch.zu.chrimametro.ui.years
import com.github.tehras.charts.bar.BarChart
import com.github.tehras.charts.bar.BarChartData
import com.github.tehras.charts.piechart.PieChart
import com.github.tehras.charts.piechart.PieChartData

private enum class CashFlowChartKind {
    Bars,
    Line,
    Pie
}

private enum class CashFlowTimeRange(
    val label: String,
    val maxItems: Int?
) {
    Last6("6M", 6),
    Last12("12M", 12),
    Last24("24M", 24),
    All("All", null)
}

@SuppressLint("DefaultLocale")
@Composable
fun CashFlowScreen(viewModel: MainViewmodel) {
    val months by viewModel.myStateFlow.collectAsState(emptyList())
    var selectedChart by remember { mutableStateOf(CashFlowChartKind.Line) }
    var selectedRange by remember { mutableStateOf(CashFlowTimeRange.All) }
    var selectedFocusYear by remember { mutableStateOf("All") }
    val visibleMonths = remember(months, selectedRange) {
        selectedRange.maxItems?.let { months.take(it) } ?: months
    }
    val monthsForRanking = remember(visibleMonths) {
        visibleMonths.drop(1).ifEmpty { visibleMonths }
    }
    val bestMonth = remember(monthsForRanking) {
        monthsForRanking.maxByOrNull { it.getNet() }
    }
    val worstMonth = remember(monthsForRanking) {
        monthsForRanking.minByOrNull { it.getNet() }
    }

    LazyColumn(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Cash Flow", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Overview, trend e dettaglio mese in un'unica vista",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (months.isEmpty()) {
            item {
                Text(
                    text = "Nessun dato disponibile.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Avg Net",
                        value = visibleMonths.map { it.getNet() }.average().toFormat() + " ₣"
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Avg Expense",
                        value = visibleMonths.map { it.getTotal() + it.fixedCosts }.average().toFormat() + " ₣"
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Avg Saving %",
                        value = visibleMonths.map { it.getPercentageCashFlow() }.average().toFormat() + "%"
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Best month",
                        value = bestMonth?.let { "${it.name} · ${it.getNet().toFormat()} ₣" } ?: "-"
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Worst month",
                        value = worstMonth?.let { "${it.name} · ${it.getNet().toFormat()} ₣" } ?: "-"
                    )
                }
            }

            item {
                CashFlowChartControls(
                    selectedChart = selectedChart,
                    onChartSelected = { selectedChart = it },
                    selectedRange = selectedRange,
                    onRangeSelected = { selectedRange = it }
                )
            }

            item {
                CashFlowMainChart(
                    months = visibleMonths,
                    chartKind = selectedChart
                )
            }

            item {
                CashFlowInsightsCard(months = visibleMonths)
            }

            item {
                FocusMonthSection(
                    months = visibleMonths,
                    selectedYear = selectedFocusYear,
                    onYearSelected = { selectedFocusYear = it }
                )
            }
        }
    }
}

@Composable
private fun FocusMonthSection(
    months: List<MonthWithdrawModel>,
    selectedYear: String,
    onYearSelected: (String) -> Unit
) {
    if (months.isEmpty()) return

    val yearOptions = remember(months) {
        val available = years.filter { year -> months.any { it.name.contains(year) } }
        listOf("All") + available
    }
    val safeYear = if (selectedYear in yearOptions) selectedYear else "All"
    val filteredMonths = remember(months, safeYear) {
        if (safeYear == "All") months else months.filter { it.name.contains(safeYear) }
    }
    if (filteredMonths.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Focus month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                yearOptions.forEach { year ->
                    val isSelected = year == safeYear
                    if (isSelected) {
                        Button(onClick = { onYearSelected(year) }) {
                            Text(year)
                        }
                    } else {
                        OutlinedButton(onClick = { onYearSelected(year) }) {
                            Text(year)
                        }
                    }
                }
            }

            Text(
                text = "Dettaglio mesi",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredMonths.forEach { month ->
                    MonthCashFlowRow(month = month)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CashFlowChartControls(
    selectedChart: CashFlowChartKind,
    onChartSelected: (CashFlowChartKind) -> Unit,
    selectedRange: CashFlowTimeRange,
    onRangeSelected: (CashFlowTimeRange) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Charts", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CashFlowChartKind.entries.forEach { kind ->
                    val isSelected = kind == selectedChart
                    if (isSelected) {
                        Button(onClick = { onChartSelected(kind) }) {
                            Text(kind.toLabel())
                        }
                    } else {
                        OutlinedButton(onClick = { onChartSelected(kind) }) {
                            Text(kind.toLabel())
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CashFlowTimeRange.entries.forEach { range ->
                    val isSelected = range == selectedRange
                    if (isSelected) {
                        Button(onClick = { onRangeSelected(range) }) {
                            Text(range.label)
                        }
                    } else {
                        OutlinedButton(onClick = { onRangeSelected(range) }) {
                            Text(range.label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CashFlowMainChart(
    months: List<MonthWithdrawModel>,
    chartKind: CashFlowChartKind
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = chartKind.toLabel(),
                style = MaterialTheme.typography.titleMedium
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                when (chartKind) {
                    CashFlowChartKind.Bars -> {
                        if (months.isEmpty()) {
                            Text("No data")
                        } else {
                            BarChart(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .padding(horizontal = 4.dp),
                                barChartData = BarChartData(
                                    bars = months.mapIndexed { index, month ->
                                        BarChartData.Bar(
                                            value = month.getNet().toFloat(),
                                            color = getCashFlowQualityColor(month.getPercentageCashFlow()),
                                            label = if (shouldShowChartLabel(index, months.size)) month.name.shortMonthLabel() else ""
                                        )
                                    }
                                )
                            )
                        }
                    }
                    CashFlowChartKind.Line -> {
                        CashFlowLineChart(
                            months = months,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            showMonthLabels = true
                        )
                    }
                    CashFlowChartKind.Pie -> {
                        val emojiCounts = months.take(12)
                            .groupingBy { getCashFlowEmoji(it.getPercentageCashFlow()) }
                            .eachCount()
                        val slices = emojiCounts.map { (emoji, count) ->
                            PieChartData.Slice(value = count.toFloat(), color = fromEmojiToColor(emoji))
                        }
                        if (slices.isEmpty()) {
                            Text("No data")
                        } else {
                            PieChart(
                                pieChartData = PieChartData(slices = slices),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QualityLegendChip(emoji = "🟢", text = ">30%")
                QualityLegendChip(emoji = "🟡", text = "20-30%")
                QualityLegendChip(emoji = "🔴", text = "<20%")
            }
        }
    }
}

private fun CashFlowChartKind.toLabel(): String = when (this) {
    CashFlowChartKind.Bars -> "Net bars"
    CashFlowChartKind.Line -> "Trend line"
    CashFlowChartKind.Pie -> "Quality pie"
}

private fun String.shortMonthLabel(): String {
    val parts = split(" ")
    if (parts.size < 2) return this
    val month = parts[0].take(3)
    val year = parts[1].takeLast(2)
    return "$month $year"
}

@Composable
private fun CashFlowLineChart(
    months: List<MonthWithdrawModel>,
    modifier: Modifier = Modifier,
    showMonthLabels: Boolean
) {
    if (months.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data")
        }
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        val lineColor = MaterialTheme.colorScheme.primary

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val values = months.map { it.getNet().toFloat() }
            val minValue = values.minOrNull() ?: 0f
            val maxValue = values.maxOrNull() ?: 0f
            val valueRange = (maxValue - minValue).takeIf { it != 0f } ?: 1f

            val horizontalPadding = if (values.size == 1) size.width * 0.5f else size.width * 0.08f
            val verticalPadding = size.height * 0.16f
            val chartWidth = size.width - horizontalPadding * 2f
            val chartHeight = size.height - verticalPadding * 2f

            fun point(index: Int, value: Float): Offset {
                val xFraction = if (values.size == 1) 0.5f else index / (values.size - 1).toFloat()
                val x = horizontalPadding + chartWidth * xFraction
                val yFraction = (value - minValue) / valueRange
                val y = verticalPadding + chartHeight * (1f - yFraction)
                return Offset(x, y)
            }

            repeat(4) { index ->
                val y = verticalPadding + chartHeight * (index / 3f)
                drawLine(
                    color = axisColor,
                    start = Offset(horizontalPadding, y),
                    end = Offset(size.width - horizontalPadding, y),
                    strokeWidth = 2f
                )
            }

            val path = Path()
            values.forEachIndexed { index, value ->
                val point = point(index, value)
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 8f)
            )

            values.forEachIndexed { index, value ->
                val point = point(index, value)
                drawCircle(
                    color = getCashFlowBackground(value.toDouble()),
                    radius = 10f,
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = point
                )
            }
        }

        if (showMonthLabels) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                months.forEachIndexed { index, month ->
                    if (shouldShowChartLabel(index, months.size)) {
                        Text(
                            text = month.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(72.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(72.dp))
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun MonthCashFlowRow(month: MonthWithdrawModel) {
    val totalExpenses = month.getTotal() + month.fixedCosts
    val percent = month.getPercentageCashFlow()
    val qualityEmoji = getCashFlowQualityEmoji(percent)
    val qualityLabel = getCashFlowQualityLabel(percent)
    val qualityColor = getCashFlowQualityColor(percent)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = month.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$qualityEmoji $qualityLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = qualityColor
                )
                Text(
                    text = "${getCashFlowEmoji(percent)}  ${percent.toFormat()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Salary: ${month.salary.toFormat()} ₣",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Expenses: ${totalExpenses.toFormat()} ₣",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Net: ${month.getNet().toFormat()} ₣",
                    style = MaterialTheme.typography.bodyMedium,
                    color = getCashFlowBackground(month.getNet())
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
fun Float.toFormat() = String.format("%.2f", args = arrayOf(this))
fun Double.toFormat() = this.toFloat().toFormat()
fun Double.toSignedFormat() = String.format("%+.2f", this)

private fun shouldShowChartLabel(index: Int, total: Int): Boolean {
    val step = when {
        total <= 6 -> 1
        total <= 12 -> 2
        total <= 24 -> 4
        else -> 6
    }
    return index == 0 || index == total - 1 || index % step == 0
}

private fun getCashFlowQualityEmoji(percent: Float): String = when {
    percent > 30f -> "🟢"
    percent >= 20f -> "🟡"
    else -> "🔴"
}

private fun getCashFlowQualityLabel(percent: Float): String = when {
    percent > 30f -> "Ottimo"
    percent >= 20f -> "Buono"
    else -> "Debole"
}

private fun getCashFlowQualityColor(percent: Float): Color = when {
    percent > 30f -> Color(0xFF4CAF50)
    percent >= 20f -> Color(0xFFFFC107)
    else -> Color(0xFFE53935)
}

@Composable
private fun CashFlowInsightsCard(months: List<MonthWithdrawModel>) {
    val bestMonth = months.maxByOrNull { it.getNet() }
    val worstMonth = months.minByOrNull { it.getNet() }
    val current12 = months.take(12)
    val previous12 = months.drop(12).take(12)
    val currentAvgPercent = current12.map { it.getPercentageCashFlow() }.average()
    val previousAvgPercent = previous12.map { it.getPercentageCashFlow() }.average()
    val trendText = if (previous12.isNotEmpty()) {
        val delta = currentAvgPercent - previousAvgPercent
        val arrow = if (delta >= 0) "↑" else "↓"
        "$arrow ${kotlin.math.abs(delta).toSignedFormat()}% vs previous 12 months"
    } else {
        "Not enough history for a 12-month comparison"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Quick insights", style = MaterialTheme.typography.titleMedium)

            if (bestMonth != null && worstMonth != null) {
                Text(
                    text = "Best month: ${bestMonth.name} (${bestMonth.getNet().toSignedFormat()} CHF)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Worst month: ${worstMonth.name} (${worstMonth.getNet().toSignedFormat()} CHF)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Trend: $trendText",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QualityLegendChip(emoji = "🟢", text = ">$30%")
                QualityLegendChip(emoji = "🟡", text = "20-30%")
                QualityLegendChip(emoji = "🔴", text = "<20%")
            }
        }
    }
}

@Composable
private fun QualityLegendChip(
    emoji: String,
    text: String
) {
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "$emoji $text",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SummaryCardWithProgress(
    textToShow: String,
    listExpenses: List<MonthWithdrawModel>,
    modifier: Modifier = Modifier
) {
    val total = if (listExpenses.isNotEmpty()) listExpenses.map { it.getTotal() + it.fixedCosts }.average() else 0.0
    val savings = if (listExpenses.isNotEmpty()) listExpenses.map { it.getNet() }.average() else 0.0
    val percentage = if (listExpenses.isNotEmpty()) listExpenses.map { it.getPercentageCashFlow() }.average() else 0.0

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = textToShow,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Spese: ${total.toFormat()} ₣",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Netto: ${savings.toFormat()} ₣",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Perc Sav: ${percentage.toFormat()}%",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ExpensesSummaryExpandable(months: List<MonthWithdrawModel>) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Summary", style = MaterialTheme.typography.titleMedium)
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand summary"
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))

                data class MonthSummary(
                    val label: String,
                    val expenses: List<MonthWithdrawModel>
                )

                val monthSummary = mutableListOf(
                    MonthSummary("Total", months),
                    MonthSummary("Last year", months.take(12)),
                    MonthSummary("Last 6 months", months.take(6))
                )
                years.forEach { year ->
                    val monthsOfYear = months.filter { it.name.contains(year) }
                    if (monthsOfYear.isNotEmpty()) {
                        monthSummary.add(MonthSummary(year, monthsOfYear))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    monthSummary.chunked(2).forEach { rowSummaries ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryCardWithProgress(
                                textToShow = rowSummaries[0].label,
                                listExpenses = rowSummaries[0].expenses,
                                modifier = Modifier.weight(1f)
                            )
                            if (rowSummaries.size == 2) {
                                SummaryCardWithProgress(
                                    textToShow = rowSummaries[1].label,
                                    listExpenses = rowSummaries[1].expenses,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnnualSummarySection(months: List<MonthWithdrawModel>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Annual summary", style = MaterialTheme.typography.titleMedium)
            years.forEach { year ->
                val monthsOfYear = months.filter { it.name.contains(year) }
                if (monthsOfYear.isNotEmpty()) {
                    Text(
                        text = "$year · Salary ${monthsOfYear[0].salary.toFormat()} ₣ · Expenses ${
                            monthsOfYear.sumOf { it.getTotal() + it.fixedCosts }.toFormat()
                        } ₣ · Net ${monthsOfYear.sumOf { it.getNet() }.toFormat()} ₣",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
