package fr.vlegall.nanoorbitapplication.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Routes(val route: String) {

    data object Dashboard : Routes("dashboard")
    data object Planning : Routes("planning")
    data object Map : Routes("map")

    data object Detail : Routes("detail/{satelliteId}") {
        fun createRoute(satelliteId: String) = "detail/$satelliteId"
        const val ARG_SATELLITE_ID = "satelliteId"
    }

    companion object {
        val bottomNavRoutes: List<BottomNavItem> = listOf(
            BottomNavItem(Dashboard, "Dashboard", Icons.Default.Dashboard),
            BottomNavItem(Planning, "Planning", Icons.Default.Schedule),
            BottomNavItem(Map, "Carte", Icons.Default.Map)
        )

        val routesWithoutBottomNav: Set<String> = setOf(
            Detail.route,
            "detail/{${Detail.ARG_SATELLITE_ID}}"
        )
    }
}

data class BottomNavItem(
    val route: Routes,
    val label: String,
    val icon: ImageVector
)