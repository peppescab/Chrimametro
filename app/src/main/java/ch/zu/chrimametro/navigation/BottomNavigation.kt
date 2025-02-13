/*
 * Copyright © 2014-2024, TWINT AG.
 * All rights reserved.
*/
package ch.zu.chrimametro.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavController) {

    val itemsNavigation = listOf(
        Screen.Earn,
        //TODO
       /* Screen.Home,
       // Screen.Month,
       // Screen.Graph,
       // Screen.Split */
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        itemsNavigation.forEach { screen ->
            NavigationBarItem(
                icon = {
                    IconHandler(screen = screen)
                },
                label = { Text(text = screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            // Avoid multiple copies of the same destination
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid building up a large stack of destinations
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
