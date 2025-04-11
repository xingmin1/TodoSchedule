package com.example.todoschedule.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

// 定义底部导航项的数据结构
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val navigateAction: () -> Unit // 导航动作
)

/**
 * 可复用的底部导航栏组件
 * @param navController NavController 用于获取当前路由
 * @param navigationState NavigationState 用于执行导航动作
 */
@Composable
fun AppBottomNavigationBar(
    navController: NavController,
    navigationState: NavigationState
) {
    // 定义所有导航项
    val items = listOf(
        BottomNavItem(
            label = "首页",
            icon = Icons.Default.Home,
            route = AppRoutes.Home.route,
            navigateAction = { navigationState.navigateToHome() }
        ),
        BottomNavItem(
            label = "课表",
            icon = Icons.Default.DateRange,
            route = AppRoutes.Schedule.route,
            navigateAction = { navigationState.navigateToSchedule() }
        ),
        BottomNavItem(
            label = "任务",
            icon = Icons.AutoMirrored.Filled.Assignment, // 使用 AutoMirrored 图标
            route = AppRoutes.Task.route,
            navigateAction = { navigationState.navigateToTask() }
        ),
        BottomNavItem(
            label = "学习",
            icon = Icons.Default.School,
            route = AppRoutes.Study.route,
            navigateAction = { navigationState.navigateToStudy() }
        ),
        BottomNavItem(
            label = "我的",
            icon = Icons.Default.Person,
            route = AppRoutes.Profile.route,
            navigateAction = { navigationState.navigateToProfile() }
        )
    )

    // 获取当前回退栈条目以确定当前路由
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.route == currentRoute,
                onClick = item.navigateAction, // 点击时执行对应的导航动作
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
                // 你可以根据需要添加颜色等自定义设置
                // colors = NavigationBarItemDefaults.colors(...)
            )
        }
    }
}
