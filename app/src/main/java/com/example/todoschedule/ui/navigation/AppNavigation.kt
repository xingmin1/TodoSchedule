package com.example.todoschedule.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.domain.model.ThemeSettings
import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.use_case.auth.GetLoginUserIdFlowUseCase
import com.example.todoschedule.ui.auth.LoginScreen
import com.example.todoschedule.ui.auth.RegisterScreen
import com.example.todoschedule.ui.course.add.AddCourseScreen
import com.example.todoschedule.ui.course.detail.CourseDetailScreen
import com.example.todoschedule.ui.course.edit.EditCourseScreen
import com.example.todoschedule.ui.course.load.SchoolSelectorScreen
import com.example.todoschedule.ui.course.load.WebViewScreen
import com.example.todoschedule.ui.home.HomeScreen
import com.example.todoschedule.ui.ordinaryschedule.AddEditOrdinaryScheduleScreen
import com.example.todoschedule.ui.ordinaryschedule.OrdinaryScheduleDetailScreen
import com.example.todoschedule.ui.schedule.ScheduleScreen
import com.example.todoschedule.ui.settings.SettingsScreen
import com.example.todoschedule.ui.table.CreateEditTableScreen
import com.example.todoschedule.ui.theme.TodoScheduleTheme
import com.example.todoschedule.ui.todo.TodoScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.net.URLDecoder
import javax.inject.Inject

/**
 * 应用导航主组件
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val navigationState = remember(navController) {
        NavigationState(navController)
    }

    // 收集主题设置状态
    val themeSettings by sessionViewModel.themeSettingsFlow.collectAsState()

    // 应用主题 - 将 themeSettings 传递给 TodoScheduleTheme
    TodoScheduleTheme(
        darkTheme = themeSettings.isDarkTheme,
        dynamicColor = themeSettings.useMaterialYou
    ) {
        // 登录状态和起始路由计算 (保持不变)
        val currentUserId by sessionViewModel.currentUserIdFlow.collectAsState()
        val startDestination = remember(currentUserId) {
            if (currentUserId != null && currentUserId != AppConstants.Ids.INVALID_USER_ID.toLong()) {
                Log.d("AppNavigation", "User logged in (ID: $currentUserId), starting at Home")
                AppRoutes.Home.route
            } else {
                Log.d("AppNavigation", "User not logged in or invalid ID, starting at Login")
                AppRoutes.Login.route
            }
        }

        // NavHost 放在 TodoScheduleTheme 内部
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            composable(AppRoutes.Login.route) {
                LoginScreen(
                    onNavigateToRegister = { navigationState.navigateToRegister() },
                    onLoginSuccess = {
                        navController.navigate(AppRoutes.Home.route) {
                            popUpTo(AppRoutes.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(AppRoutes.Register.route) {
                RegisterScreen(
                    onNavigateToLogin = { navigationState.navigateBack() },
                    onRegistrationSuccess = {
                        navController.navigate(AppRoutes.Login.route) {
                            popUpTo(AppRoutes.Login.route) { inclusive = true }
                        }
                    }
                )
            }

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
                    onNavigateBack = { navigationState.navigateBack() },
                    onLogout = {
                        navController.navigate(AppRoutes.Login.route) {
                            popUpTo(AppRoutes.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = AppRoutes.AddCourse.route,
                arguments = listOf(
                    navArgument("tableId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val tableId =
                    backStackEntry.arguments?.getInt("tableId") ?: AppConstants.Ids.INVALID_TABLE_ID
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
                    navArgument("tableId") { type = NavType.IntType },
                    navArgument("courseId") { type = NavType.IntType },
                )
            ) { backStackEntry ->
                val tableId =
                    backStackEntry.arguments?.getInt("tableId") ?: AppConstants.Ids.INVALID_TABLE_ID
                val courseId =
                    backStackEntry.arguments?.getInt("courseId")
                        ?: AppConstants.Ids.INVALID_COURSE_ID
                CourseDetailScreen(
                    tableId = tableId,
                    courseId = courseId,
                    onNavigateBack = { navigationState.navigateBack() },
                    onNavigateToEdit = { tableId, courseId ->
                        navigationState.navigateToEditCourse(tableId, courseId)
                    }
                )
            }

            // 编辑课程页面
            composable(
                route = AppRoutes.EditCourse.route,
                arguments = listOf(
                    navArgument("tableId") { type = NavType.IntType },
                    navArgument("courseId") { type = NavType.IntType },
                )
            ) { backStackEntry ->
                val tableId =
                    backStackEntry.arguments?.getInt("tableId") ?: AppConstants.Ids.INVALID_TABLE_ID
                val courseId =
                    backStackEntry.arguments?.getInt("courseId")
                        ?: AppConstants.Ids.INVALID_COURSE_ID
                EditCourseScreen(
                    tableId = tableId,
                    courseId = courseId,
                    onNavigateBack = { navigationState.navigateBack() },
                    onCourseUpdated = { navigationState.navigateBack() }
                )
            }

            // 学校选择页面
            composable(
                AppRoutes.SchoolSelector.route,
                arguments = listOf(
                    navArgument("tableId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val tableId =
                    backStackEntry.arguments?.getInt("tableId") ?: AppConstants.Ids.INVALID_TABLE_ID
                Log.d(
                    "AppNavigation",
                    "Navigating to SchoolSelector with tableId: $tableId. " +
                            "Current back stack entry args: ${backStackEntry.arguments}"
                )
                SchoolSelectorScreen(tableId = tableId, navigationState = navigationState)
            }

            //WebView页面
            composable(
                route = AppRoutes.SchoolWebView.route,
                arguments = listOf(
                    navArgument("encodedUrl") {
                        type = NavType.StringType
                        nullable = false
                    },
                    navArgument("tableId") {
                        type = NavType.IntType
                        nullable = false
                    }
                )
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("encodedUrl") ?: ""
                val originalUrl = URLDecoder.decode(encodedUrl, "UTF-8")
                val tableId =
                    backStackEntry.arguments?.getInt("tableId") ?: AppConstants.Ids.INVALID_TABLE_ID
                WebViewScreen(navigationState, originalUrl, tableId)
            }

            // 添加/编辑普通日程页面，处理可选参数
            composable(
                route = AppRoutes.AddEditOrdinarySchedule.route,
                arguments = listOf(navArgument(AppRoutes.AddEditOrdinarySchedule.ARG_SCHEDULE_ID) {
                    type = NavType.IntType
                    defaultValue = -1 // Use -1 or another invalid ID to indicate 'add' mode
                })
            ) { backStackEntry ->
                val scheduleId =
                    backStackEntry.arguments?.getInt(AppRoutes.AddEditOrdinarySchedule.ARG_SCHEDULE_ID)
                AddEditOrdinaryScheduleScreen(
                    navigationState = navigationState
                    // Pass scheduleId to ViewModel if needed for editing: scheduleId = if (scheduleId == -1) null else scheduleId
                )
            }

            // 普通日程详情页面
            composable(
                route = AppRoutes.OrdinaryScheduleDetail.route,
                arguments = listOf(navArgument(AppRoutes.OrdinaryScheduleDetail.ARG_SCHEDULE_ID) {
                    type = NavType.IntType
                })
            ) { backStackEntry ->
                val scheduleId =
                    backStackEntry.arguments?.getInt(AppRoutes.OrdinaryScheduleDetail.ARG_SCHEDULE_ID)
                requireNotNull(scheduleId) { "scheduleId parameter missing!" } // Ensure ID is present
                OrdinaryScheduleDetailScreen(
                    // scheduleId = scheduleId,
                    navigationState = navigationState
                )
            }

            // ----- 新增：创建/编辑课表页面 -----
            composable(
                route = AppRoutes.CreateEditTable.route,
                arguments = listOf(navArgument(AppRoutes.CreateEditTable.ARG_TABLE_ID) {
                    type = NavType.IntType
                    defaultValue = -1 // -1 表示创建模式
                })
            ) {
                // val tableId = it.arguments?.getInt(AppRoutes.CreateEditTable.ARG_TABLE_ID)
                // val isEditMode = tableId != -1
                CreateEditTableScreen(
                    onNavigateBack = { navigationState.navigateBack() },
                    onTableSaved = {
                        // 保存成功后返回到之前的页面（可能是 ScheduleScreen 或 Settings）
                        navigationState.navigateBack()
                        // TODO: 可能需要刷新 ScheduleScreen 的数据？
                        // 或许导航回 Schedule 并强制刷新？
                        // navController.navigate(AppRoutes.Schedule.route) { popUpTo(AppRoutes.Schedule.route){ inclusive = true } }
                    }
                )
            }
        }
    }
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    getLoginUserIdFlowUseCase: GetLoginUserIdFlowUseCase,
    private val sessionRepository: SessionRepository // 注入 SessionRepository
) : ViewModel() {
    val currentUserIdFlow: StateFlow<Long?> = getLoginUserIdFlowUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppConstants.Ids.INVALID_USER_ID.toLong())

    // 暴露 themeSettingsFlow
    val themeSettingsFlow: StateFlow<ThemeSettings> = sessionRepository.themeSettingsFlow
}
