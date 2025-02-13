/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.graphs.histogram

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun HistogramChart(
    histogramData: List<HistogramData>,
    barColor: Color = MaterialTheme.colorScheme.primary,
    onElementClicked: (String) -> Unit
) {
    val maxFrequency = histogramData.maxOf { it.frequency }
    val canvasHeight = 200.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        histogramData.forEach { data ->
            val barHeight = (data.frequency.toFloat() / maxFrequency) * canvasHeight.value

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .clickable {
                        onElementClicked(data.category)
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .height(canvasHeight)
                        .fillMaxWidth()
                        .background(Color.Transparent)
                ) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(0f, canvasHeight.toPx() - barHeight),
                        size = Size(size.width, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = data.category, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistogramPreview() {
    MaterialTheme {
        HistogramChart(
            histogramData = listOf(
                HistogramData("A", 5),
                HistogramData("B", 10),
                HistogramData("C", 3),
                HistogramData("D", 8)
            ),
            barColor = Color.Blue,
            {}
        )
    }
}
