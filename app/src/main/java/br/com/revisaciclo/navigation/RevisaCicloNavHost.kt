package br.com.revisaciclo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.revisaciclo.ui.dashboard.DashboardScreen

object Destinos {
    const val DASHBOARD = "dashboard"
}

@Composable
fun RevisaCicloNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Destinos.DASHBOARD,
    ) {
        composable(Destinos.DASHBOARD) {
            DashboardScreen()
        }
    }
}
