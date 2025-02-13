/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.tools

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    maxFontSize: TextUnit = MaterialTheme.typography.headlineLarge.fontSize,
    minFontSize: TextUnit = 12.sp,
    maxLines: Int = 1
) {
    var textSize by remember { mutableStateOf(maxFontSize) }
    var readyToDraw by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier) {
        val constraints = this.constraints
        val textMeasurer = rememberTextMeasurer()

        LaunchedEffect(text, constraints.maxWidth) {
            var fontSize = maxFontSize
            var fits = false

            while (fontSize > minFontSize && !fits) {
                val textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(text),
                    style = TextStyle(fontSize = fontSize),
                    constraints = Constraints(maxWidth = constraints.maxWidth)
                )
                if (textLayoutResult.size.width <= constraints.maxWidth) {
                    fits = true
                } else {
                    fontSize *= 0.9f
                }
            }

            textSize = fontSize
            readyToDraw = true
        }

        if (readyToDraw) {
            Text(
                text = text,
                color = color,
                fontSize = textSize,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
