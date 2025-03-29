package com.example.todoschedule.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.example.todoschedule.ui.navigation.AppNavigation
import com.example.todoschedule.ui.navigation.AppRoutes

/**
 * 应用导航图
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: AppRoutes = AppRoutes.Home
) {
    AppNavigation(navController, startDestination)
}