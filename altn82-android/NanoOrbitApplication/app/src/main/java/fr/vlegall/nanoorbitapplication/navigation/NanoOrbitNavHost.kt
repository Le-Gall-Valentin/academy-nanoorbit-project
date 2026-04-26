package fr.vlegall.nanoorbitapplication.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.vlegall.nanoorbitapplication.presentation.dashboard.DashboardScreen
import fr.vlegall.nanoorbitapplication.presentation.detail.DetailScreen
import fr.vlegall.nanoorbitapplication.presentation.map.MapScreen
import fr.vlegall.nanoorbitapplication.presentation.planning.PlanningScreen

@Composable
fun NanoOrbitNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomNav = currentDestination?.route?.let { route ->
        !Routes.routesWithoutBottomNav.any { route.startsWith("detail/") }
    } ?: true

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    Routes.bottomNavRoutes.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.route.route
                            } == true,
                            onClick = {
                                navController.navigate(item.route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Dashboard.route) {
                DashboardScreen(
                    onSatelliteClick = { id ->
                        navController.navigate(Routes.Detail.createRoute(id))
                    }
                )
            }

            composable(
                route = Routes.Detail.route,
                arguments = listOf(
                    androidx.navigation.navArgument(Routes.Detail.ARG_SATELLITE_ID) {
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val satelliteId = backStackEntry.arguments
                    ?.getString(Routes.Detail.ARG_SATELLITE_ID) ?: return@composable

                DetailScreen(
                    satelliteId = satelliteId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.Planning.route) {
                PlanningScreen()
            }

            composable(Routes.Map.route) {
                MapScreen()
            }
        }
    }
}