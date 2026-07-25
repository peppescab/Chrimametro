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
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

@SuppressLint("DefaultLocale")
@Composable
fun CashFlowScreen(viewModel: MainViewmodel) {
    val months by viewModel.myStateFlow.collectAsState(emptyList())
    var selectedChart by remember { mutableStateOf<CashFlowChartKind?>(null) }

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
                        value = months.map { it.getNet() }.average().toFormat() + " ₣"
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Avg Expense",
                        value = months.map { it.getTotal() + it.fixedCosts }.average().toFormat() + " ₣"
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Avg Saving %",
                        value = months.map { it.getPercentageCashFlow() }.average().toFormat() + "%"
                    )
                }
            }

            item {
                CashFlowInsightsCard(months = months)
            }

            item {
                ChartGallery(
                    months = months,
                    onChartSelected = { selectedChart = it }
                )
            }

            item {
                ExpensesSummaryExpandable(months)
            }

            item {
                Text(
                    text = "Monthly details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(months) { month ->
                MonthCashFlowRow(month)
            }

            item {
                AnnualSummarySection(months = months)
            }
        }
    }

    selectedChart?.let { chartKind ->
        CashFlowFullscreenChartDialog(
            chartKind = chartKind,
            months = months,
            onDismiss = { selectedChart = null }
        )
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
private fun ChartGallery(
    months: List<MonthWithdrawModel>,
    onChartSelected: (CashFlowChartKind) -> Unit
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
            Text("Charts", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ChartThumbnail(
                    title = "Net bars",
                    subtitle = "Monthly cash flow",
                    onClick = { onChartSelected(CashFlowChartKind.Bars) }
                ) {
                    BarChart(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        barChartData = BarChartData(
                            bars = months.mapIndexed { index, month ->
                                BarChartData.Bar(
                                    value = month.getNet().toFloat(),
                                    color = getCashFlowQualityColor(month.getPercentageCashFlow()),
                                    label = if (shouldShowChartLabel(index, months.size)) month.name else ""
                                )
                            }
                        )
                    )
                }

                ChartThumbnail(
                    title = "Trend line",
                    subtitle = "Cash flow over time",
                    onClick = { onChartSelected(CashFlowChartKind.Line) }
                ) {
                    CashFlowLineChart(
                        months = months,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        showMonthLabels = false
                    )
                }

                ChartThumbnail(
                    title = "Quality pie",
                    subtitle = "Last 12 months",
                    onClick = { onChartSelected(CashFlowChartKind.Pie) }
                ) {
                    val emojiCounts = months.take(12)
                        .groupingBy { getCashFlowEmoji(it.getPercentageCashFlow()) }
                        .eachCount()
                    val slices = emojiCounts.map { (emoji, count) ->
                        PieChartData.Slice(value = count.toFloat(), color = fromEmojiToColor(emoji))
                    }
                    PieChart(
                        pieChartData = PieChartData(slices = slices),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartThumbnail(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

@Composable
private fun CashFlowFullscreenChartDialog(
    chartKind: CashFlowChartKind,
    months: List<MonthWithdrawModel>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = when (chartKind) {
                                    CashFlowChartKind.Bars -> "Net bars"
                                    CashFlowChartKind.Line -> "Trend line"
                                    CashFlowChartKind.Pie -> "Quality pie"
                                },
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Pinch to zoom and drag to pan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close chart")
                        }
                    }

                    ZoomableChartStage(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                    ) {
                        when (chartKind) {
                            CashFlowChartKind.Bars -> BarChart(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                barChartData = BarChartData(
                                bars = months.mapIndexed { index, month ->
                                        BarChartData.Bar(
                                            value = month.getNet().toFloat(),
                                        color = getCashFlowQualityColor(month.getPercentageCashFlow()),
                                        label = if (shouldShowChartLabel(index, months.size)) month.name else ""
                                        )
                                    }
                                )
                            )

                            CashFlowChartKind.Line -> CashFlowLineChart(
                                months = months,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                showMonthLabels = true
                            )

                            CashFlowChartKind.Pie -> {
                                val emojiCounts = months.take(12)
                                    .groupingBy { getCashFlowEmoji(it.getPercentageCashFlow()) }
                                    .eachCount()
                                val slices = emojiCounts.map { (emoji, count) ->
                                    PieChartData.Slice(value = count.toFloat(), color = fromEmojiToColor(emoji))
                                }
                                PieChart(
                                    pieChartData = PieChartData(slices = slices),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableChartStage(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += panChange
    }

    Box(
        modifier = modifier
            .transformable(transformState)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
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
    return index == 0 || index == total - 1 || index % 4 == 0
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
    val current12 = months.takeLast(12)
    val previous12 = months.drop(maxOf(0, months.size - 24)).take(maxOf(0, months.size - 12))
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
                    MonthSummary("Last year", months.takeLast(12)),
                    MonthSummary("Last 6 months", months.takeLast(6))
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
