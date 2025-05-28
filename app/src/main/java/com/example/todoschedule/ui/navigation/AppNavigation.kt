package com.example.todoschedule.ui.navigation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
import com.example.todoschedule.ui.profile.ProfileScreen
import com.example.todoschedule.ui.schedule.ScheduleScreen
import com.example.todoschedule.ui.settings.DefaultDisplaySettingsScreen
import com.example.todoschedule.ui.settings.SettingsScreen
import com.example.todoschedule.ui.settings.SingleTableSettingsScreen
import com.example.todoschedule.ui.settings.TableManagementScreen
import com.example.todoschedule.ui.settings.TimeNodesSettingsScreen
import com.example.todoschedule.ui.study.StudyScreen
import com.example.todoschedule.ui.study.StudyViewModel
import com.example.todoschedule.ui.table.CreateEditTableScreen
import com.example.todoschedule.ui.task.TaskScreen
import com.example.todoschedule.ui.theme.TodoScheduleTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.net.URLDecoder
import javax.inject.Inject

/**
 * 应用导航主组件
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
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

    val sharedStudyViewModel: StudyViewModel = hiltViewModel()

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

        // 定义哪些路由需要显示底部导航栏
        val routesWithBottomBar = listOf(
            AppRoutes.Home.route,
            AppRoutes.Schedule.route,
            AppRoutes.Task.route,
            AppRoutes.Study.route,
            AppRoutes.Profile.route
        )
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val shouldShowBottomBar = currentRoute in routesWithBottomBar

        // --- 使用主 Scaffold 包裹 NavHost --- //
        Scaffold(
            bottomBar = {
                // 只在指定路由显示底部导航栏
                if (shouldShowBottomBar) {
                    AppBottomNavigationBar(
                        navController = navController,
                        navigationState = navigationState
                    )
                }
            }
            // topBar = { /* 如果有全局TopBar，放在这里 */ }
        ) { innerPadding -> // Scaffold 提供内边距
            // NavHost 放在 Scaffold 的 content lambda 中
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier // NavHost 本身不需要 padding，padding 应用在具体屏幕上
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
                    HomeScreen(
                        navigationState = navigationState,
                        paddingValues = innerPadding,
                        studyViewModel = sharedStudyViewModel
                    ) // 传递 padding
                }

                composable(AppRoutes.Schedule.route) {
                    ScheduleScreen(
                        navigationState = navigationState,
                        paddingValues = innerPadding
                    ) { navigationState.navigateToSettings() } // 这里传递了 innerPadding，确保课表页面有足够的底部空间

                }

                // --- 添加新屏幕的 Composable --- //
                composable(AppRoutes.Task.route) {
                    TaskScreen(
                        paddingValues = innerPadding, // 传递 padding
                        navigationState = navigationState // 传递 navigationState
                    )
                }

                composable(AppRoutes.Study.route) {
                    StudyScreen(
                        paddingValues = innerPadding,
                        viewModel = sharedStudyViewModel
                    ) // 传递 padding
                }

                composable(AppRoutes.Profile.route) {
                    ProfileScreen(
                        paddingValues = innerPadding,
                        onLogoutSuccess = {
                            navController.navigate(AppRoutes.Login.route) {
                                popUpTo(AppRoutes.Home.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        navigationState = navigationState // Pass navigationState
                        ) // 传递 padding
                }

                // --- 其他现有屏幕 --- //
                composable(AppRoutes.Settings.route) {
                    SettingsScreen(
                        onLogout = {
                            navController.navigate(AppRoutes.Login.route) {
                                popUpTo(AppRoutes.Home.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        navigationState = navigationState // Added
                    )
                }

                composable(
                    route = AppRoutes.AddCourse.route,
                    arguments = listOf(
                        navArgument("tableId") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val tableId =
                        backStackEntry.arguments?.getInt("tableId")
                            ?: AppConstants.Ids.INVALID_TABLE_ID
                    // AddCourseScreen 需要适配或有自己的 Scaffold
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
                        backStackEntry.arguments?.getInt("tableId")
                            ?: AppConstants.Ids.INVALID_TABLE_ID
                    val courseId =
                        backStackEntry.arguments?.getInt("courseId")
                            ?: AppConstants.Ids.INVALID_COURSE_ID
                    // CourseDetailScreen 需要适配或有自己的 Scaffold
                    CourseDetailScreen(
                        tableId = tableId,
                        courseId = courseId,
                        onNavigateBack = { navigationState.navigateBack() },
                        onNavigateToEdit = { tId, cId -> // Lambda 参数名修正
                            navigationState.navigateToEditCourse(tId, cId)
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
                        backStackEntry.arguments?.getInt("tableId")
                            ?: AppConstants.Ids.INVALID_TABLE_ID
                    val courseId =
                        backStackEntry.arguments?.getInt("courseId")
                            ?: AppConstants.Ids.INVALID_COURSE_ID
                    // EditCourseScreen 需要适配或有自己的 Scaffold
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
                        backStackEntry.arguments?.getInt("tableId")
                            ?: AppConstants.Ids.INVALID_TABLE_ID
                    Log.d(
                        "AppNavigation",
                        "Navigating to SchoolSelector with tableId: $tableId. " +
                                "Current back stack entry args: ${backStackEntry.arguments}"
                    )
                    // SchoolSelectorScreen 需要适配或有自己的 Scaffold
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
                        backStackEntry.arguments?.getInt("tableId")
                            ?: AppConstants.Ids.INVALID_TABLE_ID
                    // WebViewScreen 需要适配或有自己的 Scaffold
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
                    // val _scheduleId = backStackEntry.arguments?.getInt(AppRoutes.AddEditOrdinarySchedule.ARG_SCHEDULE_ID)
                    // AddEditOrdinaryScheduleScreen 需要适配或有自己的 Scaffold
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
                    // OrdinaryScheduleDetailScreen 需要适配或有自己的 Scaffold
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
                ) { /* backStackEntry -> */ // Variable 'it'/'backStackEntry' is not used
                    // val tableId = it.arguments?.getInt(AppRoutes.CreateEditTable.ARG_TABLE_ID)
                    // val isEditMode = tableId != -1
                    // CreateEditTableScreen 需要适配或有自己的 Scaffold
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

                // Settings Sub-Screens
                composable(AppRoutes.TableManagement.route) {
                    TableManagementScreen(
                        navigationState = navigationState
                    )
                }
                
                composable(AppRoutes.DefaultDisplaySettings.route) {
                    DefaultDisplaySettingsScreen(
                        navigationState = navigationState
                    )
                }
                
                composable(
                    route = AppRoutes.SingleTableSettings.route,
                    arguments = listOf(
                        navArgument(AppRoutes.SingleTableSettings.ARG_TABLE_ID) { 
                            type = NavType.StringType // TODO: Should this be IntType?
                        }
                    )
                ) {
                    SingleTableSettingsScreen(
                        navigationState = navigationState
                    )
                }
                
                // 添加 TimeNodesSettingsScreen 路由
                composable(
                    route = AppRoutes.TimeNodesSettings.route,
                    arguments = listOf(navArgument("configId") { type = NavType.IntType })
                ) {
                    TimeNodesSettingsScreen(
                        navigationState = navigationState
                    )
                }

            } // End NavHost
        } // End Scaffold content lambda
    } // End TodoScheduleTheme
} // End AppNavigation

@HiltViewModel
class SessionViewModel @Inject constructor(
    getLoginUserIdFlowUseCase: GetLoginUserIdFlowUseCase,
    sessionRepository: SessionRepository // 注入 SessionRepository
) : ViewModel() {
    val currentUserIdFlow: StateFlow<Long?> = getLoginUserIdFlowUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppConstants.Ids.INVALID_USER_ID.toLong())

    // 暴露 themeSettingsFlow
    val themeSettingsFlow: StateFlow<ThemeSettings> = sessionRepository.themeSettingsFlow
}
