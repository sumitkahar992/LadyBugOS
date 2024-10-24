package com.example.ladybugos.bottom_navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun NoteeApp() {
    val navHostController: NavHostController = rememberNavController()

    Scaffold(bottomBar = {
        NoteeBottomBar(navHostController)
    }) {
        NoteeNavigation(
            modifier = Modifier.padding(
                bottom = it.calculateBottomPadding(),
            ),
            navController = navHostController,
        )
    }

}


@Composable
fun NoteeBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination: NavDestination? = navBackStackEntry?.destination

    val showBottomNav =
        TopLevelDestinations.entries.map { it.route::class }.any { route ->
            currentDestination?.hierarchy?.any {
                it.hasRoute(route)
            } == true
        }

    AnimatedVisibility(showBottomNav) {
        BottomAppBar {
            TopLevelDestinations.entries.map { bottomNavigationItem ->

                val isSelected =
                    currentDestination?.hierarchy?.any { it.hasRoute(bottomNavigationItem.route::class) } == true

                if (currentDestination != null) {
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(bottomNavigationItem.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = bottomNavigationItem.icon,
                                contentDescription = bottomNavigationItem.label
                            )
                        },
                        alwaysShowLabel = true,
                        label = {
                            Text(bottomNavigationItem.label)
                        }
                    )
                }
            }
        }
    }
}