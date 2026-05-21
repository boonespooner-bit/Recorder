package com.recorder.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.recorder.app.ui.screens.detail.DetailScreen
import com.recorder.app.ui.screens.home.HomeScreen
import com.recorder.app.ui.screens.onboarding.OnboardingScreen
import com.recorder.app.ui.screens.record.RecordScreen
import com.recorder.app.ui.screens.signin.SignInScreen

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Record : Screen("record")
    data class Detail(val id: Long = 0) : Screen("detail/{recordingId}") {
        fun createRoute(id: Long) = "detail/$id"
        companion object {
            const val ARG = "recordingId"
        }
    }
}

@Composable
fun RecorderApp(
    signIn: () -> Unit,
    googleSignInClient: GoogleSignInClient,
    isSignedIn: Boolean
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (isSignedIn) Screen.Home.route else Screen.SignIn.route
    ) {
        composable(Screen.SignIn.route) {
            SignInScreen(
                onSignInClick = signIn,
                onSignedIn = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onRecordClick = {
                    navController.navigate(Screen.Record.route)
                },
                onRecordingClick = { id ->
                    navController.navigate(Screen.Detail().createRoute(id))
                }
            )
        }

        composable(Screen.Record.route) {
            RecordScreen(
                onRecordingComplete = { id ->
                    navController.navigate(Screen.Detail().createRoute(id)) {
                        popUpTo(Screen.Record.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Detail().route,
            arguments = listOf(
                navArgument(Screen.Detail.ARG) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong(Screen.Detail.ARG) ?: 0L
            DetailScreen(
                recordingId = id,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
