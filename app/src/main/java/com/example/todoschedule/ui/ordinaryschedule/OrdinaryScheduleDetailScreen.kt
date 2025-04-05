package com.example.todoschedule.ui.ordinaryschedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.ui.navigation.NavigationState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// TODO: Create OrdinaryScheduleDetailViewModel to fetch details

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdinaryScheduleDetailScreen(
    // scheduleId is implicitly handled by the ViewModel via SavedStateHandle
    navigationState: NavigationState,
    viewModel: OrdinaryScheduleDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Show title based on state
                    val titleText = when (val state = uiState) {
                        is OrdinaryScheduleDetailUiState.Success -> state.schedule.title
                        else -> "日程详情"
                    }
                    Text(titleText)
                },
                navigationIcon = {
                    IconButton(onClick = { navigationState.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // Show edit button only when successfully loaded
                    if (uiState is OrdinaryScheduleDetailUiState.Success) {
                        IconButton(onClick = {
                            // Navigate to AddEdit screen with the current schedule ID
                            val scheduleId =
                                (uiState as OrdinaryScheduleDetailUiState.Success).schedule.id
                            navigationState.navigateToAddEditOrdinarySchedule(scheduleId)
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.TopStart // Align content to top start
        ) {
            when (val state = uiState) {
                is OrdinaryScheduleDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is OrdinaryScheduleDetailUiState.Success -> {
                    ScheduleDetailsContent(schedule = state.schedule)
                }

                is OrdinaryScheduleDetailUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

// Composable to display the actual schedule details
@Composable
private fun ScheduleDetailsContent(schedule: OrdinarySchedule) {
    // Formatter for displaying date and time
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = schedule.title, style = MaterialTheme.typography.headlineMedium)

        if (!schedule.description.isNullOrBlank()) {
            Text(text = "描述", style = MaterialTheme.typography.titleMedium)
            Text(text = schedule.description, style = MaterialTheme.typography.bodyLarge)
        }

        if (!schedule.location.isNullOrBlank()) {
            Text(text = "地点", style = MaterialTheme.typography.titleMedium)
            Text(text = schedule.location, style = MaterialTheme.typography.bodyLarge)
        }

        // Display TimeSlots information
        if (schedule.timeSlots.isNotEmpty()) {
            Text(text = "时间", style = MaterialTheme.typography.titleMedium)
            schedule.timeSlots.forEach { timeSlot ->
                val startInstant = Instant.ofEpochMilli(timeSlot.startTime)
                val endInstant = Instant.ofEpochMilli(timeSlot.endTime)
                Text(
                    text = "从 ${dateTimeFormatter.format(startInstant)} 到 ${
                        dateTimeFormatter.format(
                            endInstant
                        )
                    }",
                    style = MaterialTheme.typography.bodyLarge
                )
                // TODO: Display more TimeSlot details if needed (e.g., reminder)
            }
        }

        // TODO: Display other fields like category, color, status etc.
    }
} 