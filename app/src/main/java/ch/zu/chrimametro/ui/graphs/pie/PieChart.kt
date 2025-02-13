/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.ui.graphs.pie

import android.graphics.Color.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PieChart(
    pieChartData: List<PieChartData>,
    modifier: Modifier = Modifier
) {
    val total = pieChartData.sumOf { it.value.toDouble() }
    var startAngle = 0f

    Canvas(modifier = modifier) {
        pieChartData.forEach { data ->
            val sweepAngle = (data.value / total) * 360f

            drawArc(
                color = data.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle.toFloat(),
                useCenter = true,
                size = Size(size.width, size.height)
            )

            startAngle = (startAngle + sweepAngle).toFloat()
        }
    }
}

@Composable
fun PieChart3D(
    pieChartData: List<PieChartData>,
    modifier: Modifier = Modifier,
    sliceThickness: Dp = 15.dp // Thickness for the 3D effect
) {
    val total = pieChartData.sumOf { it.value.toDouble() }
    var startAngle = 0f

    Canvas(modifier = modifier) {
        val canvasSize = size.minDimension
        val radius = canvasSize / 2f
        val thicknessPx = sliceThickness.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)

        pieChartData.forEach { data ->
            val sweepAngle = (data.value / total) * 360f

            // Draw the bottom slice for 3D effect
            translate(left = 0f, top = thicknessPx) {
                drawArc(
                    color = data.color.copy(alpha = 0.6f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle.toFloat(),
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
            }

            // Draw the top slice
            drawArc(
                color = data.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle.toFloat(),
                useCenter = true,
                size = Size(size.width, size.height)
            )

            // Calculate percentage and position for text
            val percentage = (data.value / total) * 100
            val angleInRad = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
            val textX = (center.x + radius * 0.7 * cos(angleInRad)).toFloat()
            val textY = (center.y + radius * 0.7 * sin(angleInRad)).toFloat()

            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    "${percentage.toInt()}%",
                    textX,
                    textY,
                    android.graphics.Paint().apply {
                        color = BLACK
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 40f
                        isFakeBoldText = true
                    }
                )
            }
            startAngle = (startAngle + sweepAngle).toFloat()
        }
    }
}

@Composable
fun SimplePieChart(
    pieChartData: List<PieChartData>,
    modifier: Modifier = Modifier
) {
    val total = pieChartData.sumOf { it.value.toDouble() }
    var startAngle = 0f

    // Group data by color and sum the values
    val groupedData = pieChartData.groupBy { it.color }
        .mapValues { entry -> entry.value.sumOf { it.value.toDouble() } }

    Canvas(modifier = modifier) {
        val canvasSize = size.minDimension
        val radius = canvasSize / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        groupedData.forEach { (color, value) ->
            val sweepAngle = (value / total) * 360f

            // Draw the slice
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle.toFloat(),
                useCenter = true,
                size = Size(size.width, size.height)
            )

            // Calculate percentage and position for text
            val percentage = (value / total) * 100
            val angleInRad = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
            val textX = (center.x + radius * 0.7 * cos(angleInRad)).toFloat()
            val textY = (center.y + radius * 0.7 * sin(angleInRad)).toFloat()

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 40f
                    isFakeBoldText = true
                }
                canvas.nativeCanvas.drawText(
                    "${percentage.toInt()}%",
                    textX,
                    textY,
                    paint
                )
            }

            startAngle = (startAngle + sweepAngle).toFloat()
        }
    }
}

data class PieChartData(
    val value: Float,
    val color: Color
)
