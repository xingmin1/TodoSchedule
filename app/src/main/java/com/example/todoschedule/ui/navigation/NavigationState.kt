package com.example.todoschedule.ui.navigation

import android.util.Log
import androidx.navigation.NavController
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.core.extensions.valid
import java.util.UUID

/**
 * 应用导航状态
 */
class NavigationState(
    val navController: NavController
) {
    /**
     * 获取当前路由
     */
    val currentRoute: String?
        get() = navController.currentDestination?.route

    /**
     * 导航到注册页面
     */
    fun navigateToRegister() {
        navController.navigate(AppRoutes.Register.route) {
            popUpTo(AppRoutes.Login.route) {
                inclusive = true
            }
        }
    }

    /**
     * 导航到首页
     */
    fun navigateToHome() {
        navController.navigate(AppRoutes.Home.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * 导航到课表页面
     */
    fun navigateToSchedule(isPop: Boolean = false) {
        navController.navigate(AppRoutes.Schedule.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * 导航到待办页面
     */
    fun navigateToTask() {
        navController.navigate(AppRoutes.Task.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * 导航到学习页面
     */
    fun navigateToStudy() {
        navController.navigate(AppRoutes.Study.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * 导航到我的页面
     */
    fun navigateToProfile() {
        navController.navigate(AppRoutes.Profile.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * 导航到设置页面
     */
    fun navigateToSettings() {
        navController.navigate(AppRoutes.Settings.route)
    }

    /**
     * 导航到登录页面
     */
    fun navigateToLogin() {
        navController.navigate(AppRoutes.Login.route) {
            popUpTo(AppRoutes.Home.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    /**
     * 导航到添加课程页面
     */
    fun navigateToAddCourse(tableId: UUID = AppConstants.Ids.INVALID_TABLE_ID) {
        navController.navigate(AppRoutes.AddCourse.createRoute(tableId))
    }

    /**
     * 导航到课程详情页面
     */
    fun navigateToCourseDetail(tableId: UUID, courseId: UUID) {
        navController.navigate(AppRoutes.CourseDetail.createRoute(tableId, courseId))
    }

    /**
     * 导航到编辑课程页面
     */
    fun navigateToEditCourse(tableId: UUID, courseId: UUID) {
        navController.navigate(AppRoutes.EditCourse.createRoute(tableId, courseId))
    }

    /**
     * 返回上一页
     */
    fun navigateBack() {
        navController.popBackStack()
    }

    /**
     * 导航到学校选择页面(传入的tableId就是最终的课表ID，要求是有效的UUID)
     */
    fun navigateSchoolSelectorScreen(tableId: UUID) {
        assert(tableId.valid())
        Log.d("NavigationState", "Navigating to SchoolSelector with tableId: $tableId")
        navController.navigate(AppRoutes.SchoolSelector.createRoute(tableId))
    }

    /**
     *导航到对应学校的WebView页面
     */
    fun navigateWebViewScreen(encodeUrl: String, tableId: UUID) {
        navController.navigate(AppRoutes.SchoolWebView.createRoute(encodeUrl, tableId))
    }

    /**
     * 导航到添加/编辑普通日程页面
     */
    fun navigateToAddEditOrdinarySchedule(scheduleId: UUID? = null) {
        val route = AppRoutes.AddEditOrdinarySchedule.createRoute(scheduleId)
        navController.navigate(route)
    }

    /**
     * 导航到普通日程详情页面
     */
    fun navigateToOrdinaryScheduleDetail(scheduleId: UUID) {
        navController.navigate(AppRoutes.OrdinaryScheduleDetail.createRoute(scheduleId))
    }

    /**
     * 导航到创建/编辑课表页面
     */
    fun navigateToCreateEditTable(tableId: UUID? = null) {
        navController.navigate(AppRoutes.CreateEditTable.createRoute(tableId))
    }

    /**
     * 导航到任务提醒选择页面
     */
    fun navigateToTaskReminder() {
        navController.navigate(AppRoutes.TaskReminder.createRoute())
    }

    /**
     * 导航到任务日历同步页面
     */
    fun navigateToTaskCalendarSync(filter: String) {
        navController.navigate(AppRoutes.TaskCalendarSync.createRoute(filter))
    }

    /**
     * 导航到同步设置页面
     */
    fun navigateToSyncSettings() {
        navController.navigate(AppRoutes.SyncSettings.route)
    }

    // Settings Sub-Screen Navigation
    fun navigateToTableManagement() {
        navController.navigate(AppRoutes.TableManagement.route)
    }

    fun navigateToDefaultDisplaySettings() {
        navController.navigate(AppRoutes.DefaultDisplaySettings.route)
    }

    fun navigateToSingleTableSettings(tableId: UUID) {
        navController.navigate(AppRoutes.SingleTableSettings.createRoute(tableId))
    }

    fun navigateToTimeNodesSettings(configId: UUID) {
        navController.navigate(AppRoutes.TimeNodesSettings.createRoute(configId))
    }
}
