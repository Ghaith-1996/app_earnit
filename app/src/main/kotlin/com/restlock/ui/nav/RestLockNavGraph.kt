package com.restlock.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.restlock.ui.AppPickerViewModel
import com.restlock.ui.HomeViewModel
import com.restlock.ui.RestLockViewModelFactory
import com.restlock.ui.SettingsViewModel
import com.restlock.ui.WorkoutViewModel
import com.restlock.ui.screens.AppPickerScreen
import com.restlock.ui.screens.HomeScreen
import com.restlock.ui.screens.PermissionOnboardingScreen
import com.restlock.ui.screens.SettingsScreen
import com.restlock.ui.screens.WorkoutScreen

object Routes {
    const val Home = "home"
    const val Workout = "workout"
    const val Settings = "settings"
    const val Permissions = "permissions"
    const val AppPicker = "app-picker"
}

@Composable
fun RestLockNavGraph(
    factory: RestLockViewModelFactory,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.Home,
) {
    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val homeState by homeViewModel.uiState.collectAsState()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.Home) {
            HomeScreen(
                viewModel = homeViewModel,
                onOpenAppPicker = { navController.navigate(Routes.AppPicker) },
                onOpenPermissions = { navController.navigate(Routes.Permissions) },
                onOpenWorkout = { navController.navigateTopLevel(Routes.Workout) },
                onOpenSettings = { navController.navigateTopLevel(Routes.Settings) },
            )
        }
        composable(Routes.Workout) {
            val workoutViewModel: WorkoutViewModel = viewModel(factory = factory)
            WorkoutScreen(
                viewModel = workoutViewModel,
                sessionActive = homeState.session.isInSession,
                onStartSavedWorkout = { workout ->
                    homeViewModel.startSavedWorkout(workout.id, homeState.chosenRest)
                    navController.navigateTopLevel(Routes.Home)
                },
                onHome = { navController.navigateTopLevel(Routes.Home) },
                onSettings = { navController.navigateTopLevel(Routes.Settings) },
            )
        }
        composable(Routes.Settings) {
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(
                viewModel = settingsViewModel,
                onHome = { navController.navigateTopLevel(Routes.Home) },
                onWorkouts = { navController.navigateTopLevel(Routes.Workout) },
            )
        }
        composable(Routes.Permissions) {
            PermissionOnboardingScreen(
                viewModel = homeViewModel,
                onContinue = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Permissions) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(Routes.AppPicker) {
            val pickerViewModel: AppPickerViewModel = viewModel(factory = factory)
            AppPickerScreen(
                viewModel = pickerViewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(Routes.Home) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
