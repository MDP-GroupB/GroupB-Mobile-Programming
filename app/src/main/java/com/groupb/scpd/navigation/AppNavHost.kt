package com.groupb.scpd.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.groupb.scpd.ui.booking.DiscoverScreen
import com.groupb.scpd.ui.common.PlaceholderScreen
import com.groupb.scpd.ui.home.HomeScreen
import com.groupb.scpd.ui.pass.PassScreen
import com.groupb.scpd.ui.scanner.PrepareScannerScreen
import com.groupb.scpd.ui.signin.SignInScreen
import com.groupb.scpd.ui.signin.SignInViewModel

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    val signInViewModel: SignInViewModel =
        viewModel(factory = SignInViewModel.Factory)

    val signInState by signInViewModel.state.collectAsStateWithLifecycle()
    val profile = signInState.profile

    fun goToSignIn() {
        navController.navigate(Routes.SIGN_IN) {
            popUpTo(navController.graph.id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SIGN_IN
    ) {

        // Megan's Firebase sign-in screen
        composable(Routes.SIGN_IN) {
            LaunchedEffect(profile) {
                if (profile != null) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SIGN_IN) {
                            inclusive = true
                        }
                    }
                }
            }

            SignInScreen(
                state = signInState,
                onEmailChange = signInViewModel::onEmailChange,
                onPasswordChange = signInViewModel::onPasswordChange,
                onSignIn = signInViewModel::signIn
            )
        }

        // Megan's Home screen and role navigation
        composable(Routes.HOME) {
            if (profile == null) {
                LaunchedEffect(Unit) {
                    goToSignIn()
                }
            } else {
                HomeScreen(
                    profile = profile,
                    onDiscover = {
                        navController.navigate(Routes.STUDENT_HOME)
                    },
                    onManageEvents = {
                        navController.navigate(Routes.CLUB_HOME)
                    },
                    onScanner = {
                        navController.navigate(Routes.SCANNER_HOME)
                    },
                    onSignOut = {
                        signInViewModel.signOut()
                    }
                )
            }
        }

        composable(Routes.STUDENT_HOME) {
            DiscoverScreen()
        }

        composable(Routes.CLUB_HOME) {
            PlaceholderScreen("Manage events (Area A)")
        }

        composable(Routes.SCANNER_HOME) {
            PrepareScannerScreen(
                onStartScanning = { _ ->
                    // Scanner navigation will be added in Week 3
                }
            )
        }

        composable(Routes.PASS) {
            PassScreen()
        }
    }
}