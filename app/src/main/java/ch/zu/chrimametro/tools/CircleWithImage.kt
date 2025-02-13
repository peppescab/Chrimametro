/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.tools

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CircleWithImage(
    image: Painter,            // Image to be displayed
    backgroundColor: Color,    // Background color for the circle
    size: Dp = 56.dp    // Background color for the circle
) {
    Box(
        modifier = Modifier
            .size(size)                               // Set the size of the circle
            .clip(CircleShape)                        // Clip to a circle
            .background(backgroundColor),             // Set the background color
        contentAlignment = Alignment.Center           // Center the image inside the circle
    ) {
        Image(
            painter = image,
            contentDescription = null,
            modifier = Modifier
                .size(size * 0.5f)                    // Scale the image to fit inside the circle
                .clip(CircleShape),                   // Ensure the image is clipped as a circle
            contentScale = ContentScale.Crop          // Crop the image to fit the size
        )
    }
}
