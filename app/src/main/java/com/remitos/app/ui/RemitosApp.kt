package com.remitos.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.remitos.app.ui.screens.DashboardScreen
import com.remitos.app.ui.screens.InboundScanScreen
import com.remitos.app.ui.screens.OutboundListScreen

@Composable
fun RemitosApp() {
    val navController = rememberNavController()
    MaterialTheme {
        Surface(modifier = Modifier) {
            AppNavHost(navController)
        }
    }
}

private object Routes {
    const val Dashboard = "dashboard"
    const val InboundScan = "inbound_scan"
    const val OutboundList = "outbound_list"
}

@Composable
private fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.Dashboard) {
        composable(Routes.Dashboard) {
            DashboardScreen(
                onScan = { navController.navigate(Routes.InboundScan) },
                onNewOutbound = { navController.navigate(Routes.OutboundList) }
            )
        }
        composable(Routes.InboundScan) {
            InboundScanScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.OutboundList) {
            OutboundListScreen(onBack = { navController.popBackStack() })
        }
    }
}
