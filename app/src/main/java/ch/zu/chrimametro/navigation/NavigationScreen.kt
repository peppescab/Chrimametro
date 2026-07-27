/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ch.zu.chrimametro.R

sealed class Screen(
    val route: String,
    val label: String,
    val iconRes: Any? = null
) {
    object HomeScreen : Screen("home", "Home", Icons.Default.Home)

    object EarnScreen : Screen("earn", "Earnings", R.drawable.ic_wallet)

    object CashFlowScreen : Screen("cash", "CashFlow", R.drawable.ic_bank)
}

@Composable
fun IconHandler(screen: Screen) {
    when (val icon = screen.iconRes) {
        is ImageVector -> {
            Icon(
                imageVector = icon,
                contentDescription = screen.label,
                modifier = Modifier.size(20.dp)
            )
        }

        is Int -> {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = screen.label,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
