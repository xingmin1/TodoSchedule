package com.example.todoschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.todoschedule.navigation.NavGraph
import com.example.todoschedule.ui.theme.TodoScheduleTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主活动
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodoScheduleTheme {
                // 创建一个表面容器，应用Material Design风格
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 创建导航控制器
                    val navController = rememberNavController()
                    
                    // 设置导航图
                    NavGraph(navController = navController)
                }
            }
        }
    }
}