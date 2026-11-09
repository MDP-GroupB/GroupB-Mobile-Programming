package com.groupb.scpd.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.groupb.scpd.ui.common.PlaceholderScreen

/** The app's map of screens. Real screens replace the placeholders area by area. */
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SIGN_IN) {
        composable(Routes.SIGN_IN) {
            PlaceholderScreen(
                title = "Sign in (Area A)",
                actions = listOf(
                    "Student" to { navController.navigate(Routes.STUDENT_HOME) },
                    "Club manager" to { navController.navigate(Routes.CLUB_HOME) },
                    "Committee scanner" to { navController.navigate(Routes.SCANNER_HOME) },
                ),
            )
        }
        composable(Routes.STUDENT_HOME) { PlaceholderScreen("Discover events (Area B)") }
        composable(Routes.CLUB_HOME) { PlaceholderScreen("Manage events (Area A)") }
        composable(Routes.SCANNER_HOME) { PlaceholderScreen("Scanner (Area D)") }
    }
}