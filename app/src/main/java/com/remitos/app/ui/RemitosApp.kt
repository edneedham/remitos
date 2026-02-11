package com.remitos.app.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.remitos.app.ui.screens.DashboardScreen
import com.remitos.app.ui.screens.InboundCameraScreen
import com.remitos.app.ui.screens.InboundHistoryScreen
import com.remitos.app.ui.screens.InboundScanScreen
import com.remitos.app.ui.screens.OutboundHistoryScreen
import com.remitos.app.ui.screens.OutboundListScreen
import com.remitos.app.ui.screens.OutboundPreviewScreen
import com.remitos.app.ui.screens.SettingsScreen
import com.remitos.app.ui.screens.SplashScreen
import com.remitos.app.ui.theme.RemitosTheme

@Composable
fun RemitosApp() {
    val navController = rememberNavController()
    RemitosTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavHost(navController)
        }
    }
}

private object Routes {
    const val Splash = "splash"
    const val Dashboard = "dashboard"
    const val InboundScan = "inbound_scan"
    const val InboundCamera = "inbound_camera"
    const val InboundHistory = "inbound_history"
    const val OutboundList = "outbound_list"
    const val OutboundHistory = "outbound_history"
    const val OutboundPreview = "outbound_preview"
    const val Settings = "settings"
}

private const val NAV_ANIM_DURATION = 300

@Composable
private fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Splash,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(NAV_ANIM_DURATION),
            ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(NAV_ANIM_DURATION),
            ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(NAV_ANIM_DURATION),
            ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(NAV_ANIM_DURATION),
            ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
        },
    ) {
        composable(
            route = Routes.Splash,
            enterTransition = { fadeIn(animationSpec = tween(0)) },
            exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) },
        ) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.Dashboard) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Dashboard) {
            DashboardScreen(
                onScan = { navController.navigate(Routes.InboundScan) },
                onInboundHistory = { navController.navigate(Routes.InboundHistory) },
                onNewOutbound = { navController.navigate(Routes.OutboundList) },
                onOutboundHistory = { navController.navigate(Routes.OutboundHistory) },
                onSettings = { navController.navigate(Routes.Settings) },
            )
        }
        composable(Routes.InboundScan) {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val capturedUri = backStackEntry
                ?.savedStateHandle
                ?.get<Uri>("captured_photo_uri")

            InboundScanScreen(
                onBack = { navController.popBackStack() },
                onOpenCamera = { navController.navigate(Routes.InboundCamera) },
                capturedUri = capturedUri,
                onCapturedUriHandled = {
                    backStackEntry
                        ?.savedStateHandle
                        ?.remove<Uri>("captured_photo_uri")
                },
            )
        }
        composable(Routes.InboundCamera) {
            InboundCameraScreen(
                onBack = { navController.popBackStack() },
                onPhotoCaptured = { uri ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("captured_photo_uri", uri)
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.OutboundList) {
            OutboundListScreen(
                onBack = { navController.popBackStack() },
                onPreview = { listId ->
                    navController.navigate("${Routes.OutboundPreview}/$listId")
                },
            )
        }
        composable(
            route = "${Routes.OutboundPreview}/{listId}",
            arguments = listOf(
                navArgument("listId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            ),
        ) { entry ->
            val listId = entry.arguments?.getLong("listId") ?: 0L
            OutboundPreviewScreen(
                listId = listId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.InboundHistory) {
            InboundHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.OutboundHistory) {
            OutboundHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Settings) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
