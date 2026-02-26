package com.remitos.app.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.remitos.app.RemitosApplication
import com.remitos.app.ui.screens.DashboardScreen
import com.remitos.app.ui.screens.DebugScreen
import com.remitos.app.ui.screens.InboundCameraScreen
import com.remitos.app.ui.screens.InboundDetailScreen
import com.remitos.app.ui.screens.InboundHistoryScreen
import com.remitos.app.ui.screens.InboundPreviewScreen
import com.remitos.app.ui.screens.InboundScanScreen
import com.remitos.app.ui.screens.LoginScreen
import com.remitos.app.ui.screens.OutboundPreviewSampleScreen
import com.remitos.app.ui.screens.ActivityScreen
import com.remitos.app.ui.screens.OutboundHistoryScreen
import com.remitos.app.ui.screens.OutboundListScreen
import com.remitos.app.ui.screens.OutboundPreviewScreen
import com.remitos.app.ui.screens.SettingsScreen
import com.remitos.app.ui.screens.SplashScreen
import com.remitos.app.ui.theme.RemitosTheme
import kotlinx.coroutines.launch

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
    const val Login = "login"
    const val Dashboard = "dashboard"
    const val InboundScan = "inbound_scan"
    const val InboundCamera = "inbound_camera"
    const val InboundPreview = "inbound_preview"
    const val InboundHistory = "inbound_history"
    const val InboundDetail = "inbound_detail"
    const val ChecklistSample = "checklist_sample"
    const val OutboundList = "outbound_list"
    const val OutboundHistory = "outbound_history"
    const val OutboundPreview = "outbound_preview"
    const val Activity = "activity"
    const val Settings = "settings"
    const val Debug = "debug"
}

private const val NAV_ANIM_DURATION = 300

@Composable
private fun AppNavHost(navController: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as RemitosApplication
    
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
            var isAuthenticated by remember { mutableStateOf<Boolean?>(null) }
            
            LaunchedEffect(Unit) {
                // Check if user is already logged in
                val currentUser = app.authManager.getCurrentUser()
                isAuthenticated = currentUser != null
                
                if (isAuthenticated == true) {
                    // Initialize user context
                    app.initializeCurrentUserContext()
                }
            }
            
            SplashScreen(
                onFinished = {
                    when (isAuthenticated) {
                        true -> {
                            // User is logged in, go to dashboard
                            navController.navigate(Routes.Dashboard) {
                                popUpTo(Routes.Splash) { inclusive = true }
                            }
                        }
                        false -> {
                            // No user logged in, go to login
                            navController.navigate(Routes.Login) {
                                popUpTo(Routes.Splash) { inclusive = true }
                            }
                        }
                        null -> {
                            // Still checking, stay on splash
                        }
                    }
                },
            )
        }
        
        composable(
            route = Routes.Login,
            enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) },
        ) {
            LoginScreen(
                onLoginSuccess = {
                    // Navigate to dashboard and clear back stack
                    navController.navigate(Routes.Dashboard) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
                onContinueOffline = {
                    // Navigate to dashboard in offline mode
                    navController.navigate(Routes.Dashboard) {
                        popUpTo(Routes.Login) { inclusive = true }
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
                onActivity = { navController.navigate(Routes.Activity) },
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
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("preview_photo_uri", uri)
                    navController.navigate(Routes.InboundPreview)
                },
            )
        }
        composable(Routes.InboundPreview) {
            val previewUri = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<Uri>("preview_photo_uri")
            InboundPreviewScreen(
                photoUri = previewUri,
                onBack = { navController.popBackStack() },
                onRetake = { navController.popBackStack() },
                onConfirm = { uri ->
                    navController.getBackStackEntry(Routes.InboundScan)
                        .savedStateHandle
                        .set("captured_photo_uri", uri)
                    navController.popBackStack(Routes.InboundScan, false)
                },
                onPhotoUriHandled = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.remove<Uri>("preview_photo_uri")
                },
            )
        }
        composable(Routes.OutboundList) {
            OutboundListScreen(
                onBack = { navController.popBackStack() },
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
            InboundHistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenDetail = { noteId -> navController.navigate("${Routes.InboundDetail}/$noteId") },
            )
        }
        composable(
            route = "${Routes.InboundDetail}/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
        ) { entry ->
            val noteId = entry.arguments?.getLong("noteId") ?: 0L
            InboundDetailScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.OutboundHistory) {
            OutboundHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Settings) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenDebug = { navController.navigate(Routes.Debug) },
                onLogout = {
                    // Navigate to login and clear back stack
                    navController.navigate(Routes.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Activity) {
            ActivityScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Debug) {
            DebugScreen(
                onBack = { navController.popBackStack() },
                onOpenChecklistSample = { navController.navigate(Routes.ChecklistSample) },
            )
        }
        composable(Routes.ChecklistSample) {
            OutboundPreviewSampleScreen(onBack = { navController.popBackStack() })
        }
    }
}
