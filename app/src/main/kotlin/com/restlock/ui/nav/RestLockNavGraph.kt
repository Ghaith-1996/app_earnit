package com.restlock.ui.nav

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.restlock.ui.AppPickerViewModel
import com.restlock.ui.HomeViewModel
import com.restlock.ui.RestLockViewModelFactory
import com.restlock.ui.screens.AppPickerScreen
import com.restlock.ui.screens.HomeScreen
import com.restlock.ui.screens.PermissionOnboardingScreen

object Routes {
    const val Home = "home"
    const val Permissions = "permissions"
    const val AppPicker = "app-picker"
}

@Composable
fun RestLockNavGraph(
    factory: RestLockViewModelFactory,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.Home,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.Home) {
            val homeViewModel: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = homeViewModel,
                onOpenAppPicker = { navController.navigate(Routes.AppPicker) },
                onOpenPermissions = { navController.navigate(Routes.Permissions) },
            )
        }
        composable(Routes.Permissions) {
            val homeViewModel: HomeViewModel = viewModel(factory = factory)
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
