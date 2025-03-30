package com.example.todoschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.todoschedule.data.database.DatabaseInitializer
import com.example.todoschedule.navigation.NavGraph
import com.example.todoschedule.ui.navigation.AppRoutes
import com.example.todoschedule.ui.theme.TodoScheduleTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主活动
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var databaseInitializer: DatabaseInitializer

    private var isInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 安装系统闪屏
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // 设置闪屏保持条件，等待数据库初始化
        splashScreen.setKeepOnScreenCondition { !isInitialized }

        // 在后台检查初始化状态
        // 注意：此处的数据库初始化发生在Application.onCreate之后
        // 数据库初始化流程是应用启动的第二步，在数据库清理之后执行
        lifecycleScope.launch {
            // 首先检查数据库是否已初始化
            isInitialized = databaseInitializer.isInitialized.first()
            if (!isInitialized) {
                try {
                    // 第二步：初始化数据库（插入默认数据等）
                    // 这一步骤在数据库被清空后执行，确保应用有正确的初始数据
                    databaseInitializer.initializeDatabase()
                    isInitialized = true
                } catch (_: Exception) {
                    // 即使出错也继续启动应用
                    isInitialized = true
                }
            }
        }

        setContent {
            TodoScheduleTheme {
                // 创建一个表面容器，应用Material Design风格
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 创建导航控制器
                    val navController = rememberNavController()

                    // 设置导航图 - 直接从Home开始，不再需要SplashScreen
                    NavGraph(
                        navController = navController,
                        startDestination = AppRoutes.Home
                    )
                }
            }
        }
    }
}