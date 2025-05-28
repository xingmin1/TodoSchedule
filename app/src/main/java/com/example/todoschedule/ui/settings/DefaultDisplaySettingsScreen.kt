package com.example.todoschedule.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.ui.navigation.NavigationState
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultDisplaySettingsScreen(
    navigationState: NavigationState,
    viewModel: DefaultDisplaySettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            scope.launch {
                snackbarHostState.showSnackbar("默认显示课表设置已保存")
            }
            viewModel.clearSaveSuccess()
            // Optionally navigate back after save
            // navigationState.navigateBack()
        }
    }

    // Handle general errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            scope.launch { snackbarHostState.showSnackbar("错误: $it") }
            viewModel.clearError()
        }
    }

    // Handle validation errors specifically
    LaunchedEffect(uiState.validationError) {
        uiState.validationError?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            // Keep the validation error displayed until selection changes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("默认显示课表设置") },
                navigationIcon = {
                    IconButton(onClick = { navigationState.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // Save Button
                    IconButton(
                        onClick = { viewModel.saveSettings() },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "保存设置")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Optional: Display Validation Error prominently
                    uiState.validationError?.let {
                        ErrorCard(message = it, onDismiss = { viewModel.clearValidationError() })
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1.0f), // Take remaining space
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "选择要在课表页默认显示的课表。注意：所选课表的学期时间不能重叠。",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(uiState.allTables, key = { it.id }) { table ->
                            SelectableTableItem(
                                table = table,
                                isSelected = table.id in uiState.selectedTableIds,
                                onSelectionChanged = { isSelected ->
                                    viewModel.onTableSelectionChanged(table.id, isSelected)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableTableItem(
    table: Table,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    // Helper to format date range
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT) }
    val startDateFormatted = table.startDate.toJavaLocalDate().format(dateFormatter)
    // Basic end date calculation (doesn't account for exact end-of-week, but gives an idea)
    // For precise end date, call the use case's helper or replicate logic
    val estimatedEndDate =
        table.startDate.plus(table.totalWeeks, DateTimeUnit.WEEK).minus(1, DateTimeUnit.DAY)
    val endDateFormatted = estimatedEndDate.toJavaLocalDate().format(dateFormatter)


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChanged
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = table.tableName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "学期: $startDateFormatted ~ $endDateFormatted (${table.totalWeeks}周)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorCard(message: String, onDismiss: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            onDismiss?.let {
                IconButton(onClick = it, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss error",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
} 