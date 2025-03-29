package com.example.todoschedule.ui.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.ui.navigation.NavigationState

/**
 * 编辑课程页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCourseScreen(
    courseId: Int,
    navigationState: NavigationState,
    viewModel: EditCourseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 加载课程数据
    LaunchedEffect(courseId) {
        viewModel.loadCourseData(courseId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑课程") },
                navigationIcon = {
                    IconButton(onClick = { navigationState.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState !is EditCourseUiState.Loading) {
                FloatingActionButton(
                    onClick = { 
                        viewModel.updateCourse()
                        navigationState.navigateBack() 
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = "保存")
                }
            }
        }
    ) { innerPadding ->
        when (uiState) {
            is EditCourseUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is EditCourseUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text((uiState as EditCourseUiState.Error).message)
                }
            }
            
            is EditCourseUiState.Success -> {
                val state = (uiState as EditCourseUiState.Success)
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 课程名称输入
                    OutlinedTextField(
                        value = state.courseName,
                        onValueChange = { viewModel.updateCourseName(it) },
                        label = { Text("课程名称") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                        }
                    )
                    
                    // 教师名称输入
                    OutlinedTextField(
                        value = state.teacherName,
                        onValueChange = { viewModel.updateTeacherName(it) },
                        label = { Text("教师姓名") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        }
                    )
                    
                    // 教室位置输入
                    OutlinedTextField(
                        value = state.location,
                        onValueChange = { viewModel.updateLocation(it) },
                        label = { Text("教室位置") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        }
                    )
                    
                    // 周次选择
                    WeekSelector(
                        selectedWeeks = state.selectedWeeks,
                        onWeekSelected = { week, selected ->
                            viewModel.updateSelectedWeek(week, selected)
                        }
                    )
                    
                    // 星期选择
                    DayOfWeekSelector(
                        selectedDay = state.dayOfWeek,
                        onDaySelected = { viewModel.updateDayOfWeek(it) }
                    )
                    
                    // 开始结束节数
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StartEndPeriodSelector(
                            startPeriod = state.startPeriod,
                            endPeriod = state.endPeriod,
                            onStartPeriodChanged = { viewModel.updateStartPeriod(it) },
                            onEndPeriodChanged = { viewModel.updateEndPeriod(it) }
                        )
                    }
                    
                    // 底部间距
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
} 