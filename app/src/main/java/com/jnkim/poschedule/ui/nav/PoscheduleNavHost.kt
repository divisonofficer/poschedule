package com.jnkim.poschedule.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jnkim.poschedule.data.repo.PlanRepository
import com.jnkim.poschedule.data.repo.SettingsRepository
import com.jnkim.poschedule.notifications.NotificationHelper
import com.jnkim.poschedule.ui.screens.DebugScreen
import com.jnkim.poschedule.ui.screens.GeminiOnboardingScreen
import com.jnkim.poschedule.ui.screens.LoginScreen
import com.jnkim.poschedule.ui.screens.SettingsScreen
import com.jnkim.poschedule.ui.screens.TodayScreen
import com.jnkim.poschedule.ui.screens.TidySnapScreen
import com.jnkim.poschedule.ui.viewmodel.AuthViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

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
            val context = LocalContext.current
            val settingsRepository = EntryPointAccessors.fromApplication(
                context.applicationContext,
                SettingsRepositoryEntryPoint::class.java
            ).settingsRepository()

            TodayScreen(
                viewModel = hiltViewModel(),
                settingsRepository = settingsRepository,
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
                onNavigateToDebug = { navController.navigate(Route.Debug.path) },
                onNavigateToGeminiSetup = { navController.navigate(Route.GeminiSetup.path) },
                onLogout = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.GeminiSetup.path) {
            GeminiOnboardingScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.Debug.path) {
            val context = LocalContext.current
            val notificationHelper = EntryPointAccessors.fromApplication(
                context.applicationContext,
                NotificationHelperEntryPoint::class.java
            ).notificationHelper()

            val planRepository = EntryPointAccessors.fromApplication(
                context.applicationContext,
                PlanRepositoryEntryPoint::class.java
            ).planRepository()

            DebugScreen(
                notificationHelper = notificationHelper,
                planRepository = planRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationHelperEntryPoint {
    fun notificationHelper(): NotificationHelper
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsRepositoryEntryPoint {
    fun settingsRepository(): SettingsRepository
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlanRepositoryEntryPoint {
    fun planRepository(): PlanRepository
}
