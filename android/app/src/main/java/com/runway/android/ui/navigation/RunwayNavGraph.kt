package com.runway.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.runway.android.ui.MainViewModel
import com.runway.android.ui.achievements.AchievementsScreen
import com.runway.android.ui.attempt.CourseAttemptTrackingScreen
import com.runway.android.ui.auth.login.LoginScreen
import com.runway.android.ui.share.RunShareImageScreen
import com.runway.android.ui.auth.signup.SignupScreen
import com.runway.android.ui.course.detail.CourseDetailScreen
import com.runway.android.ui.course.detail.CourseMapDetailScreen
import com.runway.android.ui.course.my.MyCoursesScreen
import com.runway.android.ui.leaderboard.CourseLeaderboardScreen
import com.runway.android.ui.onboarding.OnboardingScreen
import com.runway.android.ui.running.RunResultScreen
import com.runway.android.ui.running.RunningTrackingScreen
import com.runway.android.ui.reminder.ReminderScreen
import com.runway.android.ui.running.history.MyRunsScreen
import com.runway.android.ui.running.history.RunDetailScreen
import com.runway.android.ui.splash.RunwaySplashScreen
import com.runway.android.ui.stats.StatsScreen

@Composable
fun RunwayNavGraph() {
    val mainViewModel: MainViewModel = hiltViewModel()
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()
    val isOnboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != null &&
                currentRoute != RunwayRoutes.SPLASH &&
                currentRoute != RunwayRoutes.LOGIN &&
                currentRoute != RunwayRoutes.SIGNUP
            ) {
                navController.navigate(RunwayRoutes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = RunwayRoutes.SPLASH,
    ) {
        composable(RunwayRoutes.SPLASH) {
            RunwaySplashScreen(
                onAnimationFinished = {
                    val destination = when {
                        isLoggedIn == false -> RunwayRoutes.LOGIN
                        isLoggedIn == true && isOnboardingCompleted == false -> RunwayRoutes.ONBOARDING
                        isLoggedIn == true && isOnboardingCompleted == true -> RunwayRoutes.MAIN
                        else -> RunwayRoutes.LOGIN
                    }
                    navController.navigate(destination) {
                        popUpTo(RunwayRoutes.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        // ─── Auth ───

        composable(RunwayRoutes.LOGIN) {
            LoginScreen(
                onNavigateToSignup = { navController.navigate(RunwayRoutes.SIGNUP) },
                onLoginSuccess = {
                    val dest = if (mainViewModel.isOnboardingCompleted.value == false)
                        RunwayRoutes.ONBOARDING else RunwayRoutes.MAIN
                    navController.navigate(dest) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(RunwayRoutes.SIGNUP) {
            SignupScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignupSuccess = {
                    navController.navigate(RunwayRoutes.LOGIN) {
                        popUpTo(RunwayRoutes.SIGNUP) { inclusive = true }
                    }
                },
            )
        }

        // ─── Onboarding ───

        composable(RunwayRoutes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(RunwayRoutes.MAIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ─── Main shell (BottomNav 포함) ───

        composable(RunwayRoutes.MAIN) {
            MainScaffold(
                onStartRun = { navController.navigate(RunwayRoutes.RUNNING) },
                onLogout = {
                    navController.navigate(RunwayRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToCourseDetail = { courseId ->
                    navController.navigate(RunwayRoutes.courseDetail(courseId))
                },
                onNavigateToMyRuns = { navController.navigate(RunwayRoutes.MY_RUNS) },
                onNavigateToCourses = { navController.navigate(RunwayRoutes.MY_COURSES) },
                onNavigateToRunDetail = { runId ->
                    navController.navigate(RunwayRoutes.runDetail(runId))
                },
                onNavigateToStats = { navController.navigate(RunwayRoutes.STATS) },
                onNavigateToAchievements = { navController.navigate(RunwayRoutes.ACHIEVEMENTS) },
                onNavigateToReminder = { navController.navigate(RunwayRoutes.REMINDER) },
            )
        }

        // ─── 자유 러닝 (BottomNav 없음) ───

        composable(RunwayRoutes.RUNNING) {
            RunningTrackingScreen(
                onFinish = { runId, elapsedSeconds, distanceKm ->
                    navController.navigate(
                        RunwayRoutes.runResult(runId ?: "none", elapsedSeconds, distanceKm),
                    ) {
                        popUpTo(RunwayRoutes.RUNNING) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

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
                onShareImage = { runId ->
                    navController.navigate(RunwayRoutes.runShare(runId))
                },
                onOpenRunDetail = { runId ->
                    navController.navigate(RunwayRoutes.runDetail(runId))
                },
            )
        }

        // ─── 코스 상세 (BottomNav 없음) ───

        composable(
            route = RunwayRoutes.COURSE_DETAIL,
            arguments = listOf(navArgument("courseId") { type = NavType.StringType }),
        ) {
            CourseDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAttempt = { courseId, courseAttemptId, runningRecordId ->
                    navController.navigate(
                        RunwayRoutes.courseAttempt(courseId, courseAttemptId, runningRecordId)
                    )
                },
                onNavigateToLeaderboard = { courseId ->
                    navController.navigate(RunwayRoutes.courseLeaderboard(courseId))
                },
                onNavigateToMap = { courseId ->
                    navController.navigate(RunwayRoutes.courseMapDetail(courseId))
                },
            )
        }

        // ─── 코스 전체 지도 (BottomNav 없음) ───

        composable(
            route = RunwayRoutes.COURSE_MAP_DETAIL,
            arguments = listOf(navArgument("courseId") { type = NavType.StringType }),
        ) {
            CourseMapDetailScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // ─── 코스 도전 트래킹 (BottomNav 없음) ───

        composable(
            route = RunwayRoutes.COURSE_ATTEMPT,
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("courseAttemptId") { type = NavType.StringType },
                navArgument("runningRecordId") { type = NavType.StringType },
            ),
        ) {
            CourseAttemptTrackingScreen(
                onNavigateToLeaderboard = { courseId ->
                    navController.navigate(RunwayRoutes.courseLeaderboard(courseId)) {
                        popUpTo(RunwayRoutes.COURSE_ATTEMPT) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ─── 내 러닝 기록 목록 (BottomNav 없음) ───

        composable(RunwayRoutes.MY_RUNS) {
            MyRunsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { runId ->
                    navController.navigate(RunwayRoutes.runDetail(runId))
                },
            )
        }

        // ─── 러닝 기록 상세 (BottomNav 없음) ───

        composable(
            route = RunwayRoutes.RUN_DETAIL,
            arguments = listOf(navArgument("runId") { type = NavType.StringType }),
        ) {
            RunDetailScreen(
                onBack = { navController.popBackStack() },
                onShareImage = { runId ->
                    navController.navigate(RunwayRoutes.runShare(runId))
                },
            )
        }

        // ─── 내 코스 목록 (BottomNav 없음) ───

        composable(RunwayRoutes.MY_COURSES) {
            MyCoursesScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCourseDetail = { courseId ->
                    navController.navigate(RunwayRoutes.courseDetail(courseId))
                },
            )
        }

        // ─── 코스 리더보드 (BottomNav 없음) ───

        composable(
            route = RunwayRoutes.COURSE_LEADERBOARD,
            arguments = listOf(navArgument("courseId") { type = NavType.StringType }),
        ) {
            CourseLeaderboardScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // ─── 공유 이미지 생성 (BottomNav 없음) ───

        composable(
            route = RunwayRoutes.RUN_SHARE,
            arguments = listOf(navArgument("runId") { type = NavType.StringType }),
        ) {
            RunShareImageScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // ─── 통계 ───

        composable(RunwayRoutes.STATS) {
            StatsScreen(onBack = { navController.popBackStack() })
        }

        // ─── 업적 ───

        composable(RunwayRoutes.ACHIEVEMENTS) {
            AchievementsScreen(onBack = { navController.popBackStack() })
        }

        // ─── 리마인더 ───

        composable(RunwayRoutes.REMINDER) {
            ReminderScreen(onBack = { navController.popBackStack() })
        }
    }
}
