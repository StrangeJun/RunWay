package com.runway.android.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.runway.android.ui.MainViewModel
import com.runway.android.ui.auth.login.LoginScreen
import com.runway.android.ui.auth.signup.SignupScreen
import com.runway.android.ui.running.RunResultScreen
import com.runway.android.ui.running.RunningTrackingScreen

@Composable
fun RunwayNavGraph() {
    val mainViewModel: MainViewModel = hiltViewModel()
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()

    // Wait for DataStore to emit the first value before rendering NavHost.
    // Prevents a flash of the login screen for already-authenticated users.
    if (isLoggedIn == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
        return
    }

    val startDestination = if (isLoggedIn == true) RunwayRoutes.MAIN else RunwayRoutes.LOGIN
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        // ─── Auth ───

        composable(RunwayRoutes.LOGIN) {
            LoginScreen(
                onNavigateToSignup = {
                    navController.navigate(RunwayRoutes.SIGNUP)
                },
                onLoginSuccess = {
                    navController.navigate(RunwayRoutes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(RunwayRoutes.SIGNUP) {
            SignupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignupSuccess = {
                    navController.navigate(RunwayRoutes.LOGIN) {
                        popUpTo(RunwayRoutes.SIGNUP) { inclusive = true }
                    }
                },
            )
        }

        // ─── Main shell (with BottomNav) ───

        composable(RunwayRoutes.MAIN) {
            MainScaffold(
                onStartRun = {
                    navController.navigate(RunwayRoutes.RUNNING)
                },
                onLogout = {
                    navController.navigate(RunwayRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ─── Running (no BottomNav — outside MainScaffold) ───

        composable(RunwayRoutes.RUNNING) {
            RunningTrackingScreen(
                onFinish = { runId, elapsedSeconds, distanceKm ->
                    val safeRunId = runId ?: "none"
                    navController.navigate(
                        "${RunwayRoutes.RUN_RESULT}/$safeRunId/$elapsedSeconds/$distanceKm",
                    ) {
                        popUpTo(RunwayRoutes.RUNNING) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                },
            )
        }

        // ─── Run result (no BottomNav — outside MainScaffold) ───

        composable(
            route = "${RunwayRoutes.RUN_RESULT}/{runId}/{elapsedSeconds}/{distanceKm}",
            arguments = listOf(
                navArgument("runId") { type = NavType.StringType },
                navArgument("elapsedSeconds") { type = NavType.IntType },
                navArgument("distanceKm") { type = NavType.FloatType },
            ),
        ) {
            RunResultScreen(
                onBackToHome = {
                    navController.popBackStack(RunwayRoutes.MAIN, inclusive = false)
                },
            )
        }
    }
}
