/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ch.zu.chrimametro.R

sealed class Screen(val route: String, val label: String, val icon: Any) {
    data object Earn : Screen("chrimametro", "Chrima", Icons.Default.PlayArrow)
    //TODO
    /*
    data object Home : Screen("expense", "Expense", Icons.Filled.DateRange)
    data object Budget : Screen("budget", "Settings", R.drawable.ic_wallet)
    data object Month : Screen("month", "Budget", R.drawable.ic_wallet)
    data object Graph : Screen("graph", "Graph", R.drawable.ic_graph)
    data object Split : Screen("split", "Split", android.R.drawable.ic_menu_compass)

     */
}

@Composable
fun IconHandler(screen: Screen) {
    when (val icon = screen.icon) {
        is ImageVector -> {
            Icon(
                imageVector = icon,
                contentDescription = screen.label,
                modifier = Modifier.size(24.dp)
            )
        }

        is Int -> { // Resource ID for the drawable
            Icon(
                painter = painterResource(id = icon),
                contentDescription = screen.label,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
