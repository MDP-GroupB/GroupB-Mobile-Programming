package com.groupb.scpd.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.groupb.scpd.ui.common.PlaceholderScreen
import com.groupb.scpd.ui.home.HomeScreen
import com.groupb.scpd.ui.signin.SignInScreen
import com.groupb.scpd.ui.signin.SignInViewModel

/** The app's map of screens. Real screens replace the placeholders area by area. */
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    // One SignInViewModel for the whole app, built by its Factory (see AppContainer),
    // so Home can read the signed-in profile.
    val signInViewModel: SignInViewModel = viewModel(factory = SignInViewModel.Factory)
    val signInState by signInViewModel.state.collectAsState()
    val profile = signInState.profile

    fun goToSignIn() {
        navController.navigate(Routes.SIGN_IN) {
            popUpTo(navController.graph.id) { inclusive = true }   // clear every earlier screen
        }
    }

    NavHost(navController = navController, startDestination = Routes.SIGN_IN) {
        composable(Routes.SIGN_IN) {
            // When sign-in succeeds the profile appears, so move on to Home.
            LaunchedEffect(profile) {
                if (profile != null) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SIGN_IN) { inclusive = true }   // Back must not return to sign-in
                    }
                }
            }
            SignInScreen(
                state = signInState,
                onEmailChange = signInViewModel::onEmailChange,
                onPasswordChange = signInViewModel::onPasswordChange,
                onSignIn = signInViewModel::signIn,
            )
        }
        composable(Routes.HOME) {
            if (profile == null) {
                LaunchedEffect(Unit) { goToSignIn() }   // not signed in: back to sign-in
            } else {
                HomeScreen(
                    profile = profile,
                    onDiscover = { navController.navigate(Routes.STUDENT_HOME) },
                    onManageEvents = { navController.navigate(Routes.CLUB_HOME) },
                    onScanner = { navController.navigate(Routes.SCANNER_HOME) },
                    onSignOut = {
                        signInViewModel.signOut()
                        goToSignIn()
                    },
                )
            }
        }
        composable(Routes.STUDENT_HOME) { PlaceholderScreen("Discover events (Area B)") }
        composable(Routes.CLUB_HOME) { PlaceholderScreen("Manage events (Area A)") }
        composable(Routes.SCANNER_HOME) { PlaceholderScreen("Scanner (Area D)") }
    }
}