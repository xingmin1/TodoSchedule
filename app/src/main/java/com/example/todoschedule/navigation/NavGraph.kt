package com.example.todoschedule.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.example.todoschedule.ui.navigation.AppNavigation

/**
 * 应用导航图
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun NavGraph(
    navController: NavHostController,
) {
    AppNavigation(navController)
}