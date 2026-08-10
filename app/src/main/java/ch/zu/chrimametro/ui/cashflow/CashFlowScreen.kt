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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.zu.chrimametro.Utils.getCurrentMonth
import ch.zu.chrimametro.ui.expense.MainViewmodel
import ch.zu.chrimametro.ui.expense.MonthWithdrawModel
import ch.zu.chrimametro.ui.fromEmojiToColor
import ch.zu.chrimametro.ui.getCashFlowBackground
import ch.zu.chrimametro.ui.getCashFlowEmoji
import ch.zu.chrimametro.ui.years
import com.github.tehras.charts.piechart.PieChart
import com.github.tehras.charts.piechart.PieChartData
import kotlin.math.max

private enum class CashFlowChartKind {
    Bars,
    Line,
    Pie
}

private enum class CashFlowTimeRange(
    val label: String,
    val maxItems: Int?
) {
    Last3("3M", 3),
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
    // Exclude the most recent month from all stats & charts
    val completedMonths = remember(months) {
        if (months.size > 1) months.drop(1) else emptyList()
    }
    val visibleMonths = remember(completedMonths, selectedRange) {
        selectedRange.maxItems?.let { completedMonths.take(it) } ?: completedMonths
    }
    val chartMonths = visibleMonths
    val monthsForRanking = visibleMonths
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
                CashFlowStatsOverview(months = visibleMonths, bestMonth = bestMonth, worstMonth = worstMonth)
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
                    months = chartMonths,
                    chartKind = selectedChart
                )
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
private fun CashFlowStatsOverview(
    months: List<MonthWithdrawModel>,
    bestMonth: MonthWithdrawModel?,
    worstMonth: MonthWithdrawModel?
) {
    if (months.isEmpty()) return

    val avgNet = months.map { it.getNet() }.average()
    val avgExpense = months.map { it.getTotal() + it.fixedCosts }.average()
    val avgSaving = months.map { it.getPercentageCashFlow() }.average()
    val totalSaved = months.sumOf { it.getNet().toDouble() }
    val greenCount = months.count { it.getPercentageCashFlow() > 30f }
    val yellowCount = months.count { it.getPercentageCashFlow() in 20f..30f }
    val redCount = months.count { it.getPercentageCashFlow() < 20f }

    // Trend: last 6 vs previous 6
    val recent6 = months.take(6)
    val prev6 = months.drop(6).take(6)
    val trendDelta = if (prev6.isNotEmpty()) {
        recent6.map { it.getPercentageCashFlow() }.average() - prev6.map { it.getPercentageCashFlow() }.average()
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with trend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Statistiche",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (trendDelta != null) {
                    val arrow = if (trendDelta >= 0) "📈" else "📉"
                    val trendColor = if (trendDelta >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    Text(
                        text = "Trend 6M $arrow ${kotlin.math.abs(trendDelta).toFormat()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = trendColor
                    )
                }
            }

            // Main KPIs row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatKpi(
                    modifier = Modifier.weight(1f),
                    emoji = "💰",
                    label = "Netto medio",
                    value = "${avgNet.toFormat()} ₣",
                    valueColor = if (avgNet >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
                StatKpi(
                    modifier = Modifier.weight(1f),
                    emoji = "🔥",
                    label = "Spesa media",
                    value = "${avgExpense.toFormat()} ₣",
                    valueColor = MaterialTheme.colorScheme.onSurface
                )
                StatKpi(
                    modifier = Modifier.weight(1f),
                    emoji = "🎯",
                    label = "Saving %",
                    value = "${avgSaving.toFormat()}%",
                    valueColor = when {
                        avgSaving > 30 -> Color(0xFF4CAF50)
                        avgSaving > 20 -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }

            // Total saved + month quality distribution
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatKpi(
                    modifier = Modifier.weight(1f),
                    emoji = "🏦",
                    label = "Totale risparmiato",
                    value = "${totalSaved.toFormat()} ₣",
                    valueColor = if (totalSaved >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🟢 $greenCount", style = MaterialTheme.typography.labelLarge)
                        Text("🟡 $yellowCount", style = MaterialTheme.typography.labelLarge)
                        Text("🔴 $redCount", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Best & Worst
            if (bestMonth != null && worstMonth != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatHighlight(
                        modifier = Modifier.weight(1f),
                        emoji = "🏆",
                        label = bestMonth.name,
                        value = "+${bestMonth.getNet().toFormat()} ₣",
                        bgColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                    StatHighlight(
                        modifier = Modifier.weight(1f),
                        emoji = "⚠️",
                        label = worstMonth.name,
                        value = "${worstMonth.getNet().toFormat()} ₣",
                        bgColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatKpi(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    value: String,
    valueColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = "$emoji $label", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun StatHighlight(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    value: String,
    bgColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
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
            when (chartKind) {
                CashFlowChartKind.Bars,
                CashFlowChartKind.Line -> {
                    val entries = months.reversed().map { month ->
                        ChartEntry(
                            label = month.name.shortMonthLabel(),
                            fullLabel = month.name,
                            value = month.getNet().toFloat(),
                            color = getCashFlowQualityColor(month.getPercentageCashFlow())
                        )
                    }
                    NiceMonthlyChart(
                        entries = entries,
                        kind = chartKind
                    )
                }
                CashFlowChartKind.Pie -> {
                    val emojiCounts = months.take(12)
                        .groupingBy { getCashFlowEmoji(it.getPercentageCashFlow()) }
                        .eachCount()
                    val slices = emojiCounts.map { (emoji, count) ->
                        PieChartData.Slice(value = count.toFloat(), color = fromEmojiToColor(emoji))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (slices.isEmpty()) {
                            Text("No data")
                        } else {
                            PieChart(
                                pieChartData = PieChartData(slices = slices),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
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

private data class ChartEntry(
    val label: String,
    val fullLabel: String,
    val value: Float,
    val color: Color
)

@Composable
private fun NiceMonthlyChart(
    entries: List<ChartEntry>,
    kind: CashFlowChartKind
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No data")
        }
        return
    }

    var selectedIndex by remember(entries) { mutableStateOf<Int?>(null) }
    val values = entries.map { it.value }
    val rawMin = values.min()
    val rawMax = values.max()
    val paddedMin = if (rawMin > 0f) 0f else rawMin - kotlin.math.abs(rawMin) * 0.1f
    val paddedMax = if (rawMax < 0f) 0f else rawMax + kotlin.math.abs(rawMax) * 0.1f
    val range = (paddedMax - paddedMin).takeIf { it != 0f } ?: 1f
    val steps = 5
    val yLabels = (steps downTo 0).map { i -> paddedMin + range * (i / steps.toFloat()) }

    val chartHeightDp = 240.dp
    val axisWidthDp = 60.dp
    val perItemDp = if (kind == CashFlowChartKind.Bars) 44 else 56
    val contentWidthDp = max(280, entries.size * perItemDp).dp

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val zeroLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxWidth()) {
        val selected = selectedIndex?.let { entries.getOrNull(it) }
        Text(
            text = if (selected != null) "${selected.fullLabel} · ${selected.value.toFormat()} ₣"
                   else "Tocca una barra/punto per il dettaglio",
            style = MaterialTheme.typography.labelMedium,
            color = if (selected != null) selected.color else axisTextColor,
            fontWeight = if (selected != null) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier
                    .width(axisWidthDp)
                    .height(chartHeightDp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yLabels.forEach { v ->
                    Text(
                        text = v.toFormat(),
                        style = MaterialTheme.typography.labelSmall,
                        color = axisTextColor,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(contentWidthDp)
                        .height(chartHeightDp)
                        .pointerInput(entries) {
                            detectTapGestures { tap ->
                                val slot = size.width.toFloat() / entries.size
                                val idx = (tap.x / slot).toInt().coerceIn(0, entries.size - 1)
                                selectedIndex = if (selectedIndex == idx) null else idx
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        yLabels.forEachIndexed { i, v ->
                            val y = h * (i / steps.toFloat())
                            drawLine(
                                color = if (kotlin.math.abs(v) < 0.001f) zeroLineColor else gridColor,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = if (kotlin.math.abs(v) < 0.001f) 3f else 2f
                            )
                        }

                        fun yFor(value: Float): Float {
                            val frac = (value - paddedMin) / range
                            return h * (1f - frac)
                        }

                        val slot = w / entries.size
                        if (kind == CashFlowChartKind.Bars) {
                            val barPadding = slot * 0.18f
                            val barWidth = slot - barPadding * 2f
                            entries.forEachIndexed { i, entry ->
                                val x0 = slot * i + barPadding
                                val yTop = yFor(entry.value)
                                val zeroY = yFor(0f)
                                val top = kotlin.math.min(yTop, zeroY)
                                val bot = kotlin.math.max(yTop, zeroY)
                                val isSelected = selectedIndex == i
                                drawRect(
                                    color = if (isSelected) entry.color else entry.color.copy(alpha = 0.75f),
                                    topLeft = Offset(x0, top),
                                    size = Size(barWidth, (bot - top).coerceAtLeast(2f))
                                )
                            }
                        } else {
                            fun pt(i: Int, v: Float) = Offset(slot * i + slot / 2f, yFor(v))
                            val path = Path()
                            entries.forEachIndexed { i, e ->
                                val p = pt(i, e.value)
                                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                            }
                            drawPath(path = path, color = lineColor, style = Stroke(width = 6f))
                            entries.forEachIndexed { i, e ->
                                val p = pt(i, e.value)
                                val isSelected = selectedIndex == i
                                drawCircle(color = e.color, radius = if (isSelected) 14f else 8f, center = p)
                                drawCircle(color = Color.White, radius = if (isSelected) 6f else 3f, center = p)
                            }
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(axisWidthDp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
            ) {
                Row(modifier = Modifier.width(contentWidthDp)) {
                    entries.forEachIndexed { i, entry ->
                        Text(
                            text = if (shouldShowChartLabel(i, entries.size)) entry.label else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = axisTextColor,
                            fontWeight = if (selectedIndex == i) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 6.dp)
                        )
                    }
                }
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
