package com.example.todoschedule.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.todoschedule.ui.course.add.AddCourseScreen
import com.example.todoschedule.ui.course.detail.CourseDetailScreen
import com.example.todoschedule.ui.course.edit.EditCourseScreen
import com.example.todoschedule.ui.home.HomeScreen
import com.example.todoschedule.ui.schedule.ScheduleScreen
import com.example.todoschedule.ui.settings.SettingsScreen
import com.example.todoschedule.ui.todo.TodoScreen

/**
 * 应用导航主组件
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: AppRoutes = AppRoutes.Home,
) {
    val navigationState = remember(navController) {
        NavigationState(navController)
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination.route,
    ) {
        composable(AppRoutes.Home.route) {
            HomeScreen(navigationState = navigationState)
        }
        
        composable(AppRoutes.Schedule.route) {
            ScheduleScreen(
                navigationState = navigationState,
                onNavigateToSettings = { navigationState.navigateToSettings() }
            )
        }
        
        composable(AppRoutes.Todo.route) {
            TodoScreen(navigationState = navigationState)
        }
        
        composable(AppRoutes.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navigationState.navigateBack() }
            )
        }
        
        composable(
            route = AppRoutes.AddCourse.route,
            arguments = listOf(
                navArgument("tableId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val tableId = backStackEntry.arguments?.getInt("tableId") ?: 1
            AddCourseScreen(
                tableId = tableId,
                onNavigateBack = { navigationState.navigateBack() },
                onCourseAdded = { navigationState.navigateBack() }
            )
        }
        
        // 课程详情页面
        composable(
            route = AppRoutes.CourseDetail.route,
            arguments = listOf(
                navArgument("courseId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getInt("courseId") ?: 0
            CourseDetailScreen(
                courseId = courseId,
                onNavigateBack = { navigationState.navigateBack() },
                onNavigateToEdit = { courseId -> 
                    navigationState.navigateToEditCourse(courseId) 
                }
            )
        }
        
        // 编辑课程页面
        composable(
            route = AppRoutes.EditCourse.route,
            arguments = listOf(
                navArgument("courseId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getInt("courseId") ?: 0
            EditCourseScreen(
                courseId = courseId,
                onNavigateBack = { navigationState.navigateBack() },
                onCourseUpdated = { navigationState.navigateBack() }
            )
        }
        
        // 添加更多路由...
    }
} 