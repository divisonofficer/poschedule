package com.jnkim.poschedule.ui.nav

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jnkim.poschedule.ui.screens.LoginScreen
import com.jnkim.poschedule.ui.screens.SettingsScreen
import com.jnkim.poschedule.ui.screens.TodayScreen
import com.jnkim.poschedule.ui.screens.TidySnapScreen
import com.jnkim.poschedule.ui.viewmodel.AuthViewModel

@Composable
fun PoscheduleNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Route.Login.path) {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Route.Today.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Today.path) {
            TodayScreen(
                viewModel = hiltViewModel(),
                onNavigateToSettings = { navController.navigate(Route.Settings.path) },
                onNavigateToTidySnap = { navController.navigate(Route.TidySnap.path) }
            )
        }

        composable(Route.TidySnap.path) {
            TidySnapScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(Route.Settings.path) {
            SettingsScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
