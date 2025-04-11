package com.example.todoschedule.ui.ordinaryschedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.ui.navigation.NavigationState
import com.example.todoschedule.ui.theme.getColorListFromColorScheme
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditOrdinaryScheduleScreen(
    navigationState: NavigationState,
    viewModel: AddEditOrdinaryScheduleViewModel = hiltViewModel()
) {
    val courseScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- State for Pickers --- (Create separate states with keys)

    // Start Date Picker State
    val startDatePickerState = remember(uiState.startDate) { // Keyed to uiState.startDate
        DatePickerState(
            initialSelectedDateMillis = uiState.startDate?.atStartOfDayIn(TimeZone.currentSystemDefault())
                ?.toEpochMilliseconds()
                ?: Clock.System.now().toEpochMilliseconds(), // Default to now if null
            // yearRange = ... // Optional: restrict year range
            locale = java.util.Locale.getDefault()
        )
    }

    // Start Time Picker State
    val startTimePickerState = remember(uiState.startTime) { // Keyed to uiState.startTime
        val initialHour = uiState.startTime?.hour ?: Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).hour
        val initialMinute = uiState.startTime?.minute ?: Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).minute
        TimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    }

    // End Date Picker State
    val endDatePickerState = remember(uiState.endDate) { // Keyed to uiState.endDate
        DatePickerState(
            initialSelectedDateMillis = uiState.endDate?.atStartOfDayIn(TimeZone.currentSystemDefault())
                ?.toEpochMilliseconds()
                ?: uiState.startDate?.atStartOfDayIn(TimeZone.currentSystemDefault())
                    ?.toEpochMilliseconds()
                ?: Clock.System.now().toEpochMilliseconds(), // Or now if start date is also null
            locale = java.util.Locale.getDefault()
            // yearRange = ...
        )
    }

    // End Time Picker State
    val endTimePickerState = remember(uiState.endTime) { // Keyed to uiState.endTime
        val initialHour = uiState.endTime?.hour ?: uiState.startTime?.hour ?: Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).hour
        val initialMinute =
            uiState.endTime?.minute ?: uiState.startTime?.minute ?: Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).minute
        TimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    }

    // --- Event Handling ---
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            navigationState.navigateBack()
            viewModel.consumeSavedEvent() // 重置事件状态
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            snackbarHostState.showSnackbar(
                message = uiState.errorMessage!!,
                duration = SnackbarDuration.Short
            )
            viewModel.consumeErrorMessage() // 消费错误消息
        }
    }

    // --- Dialogs --- (Use corresponding states)
    if (uiState.showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.dismissStartDatePicker() },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let { // Use startDatePickerState
                        viewModel.onStartDateSelected(
                            Instant.fromEpochMilliseconds(it)
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date
                        )
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { viewModel.dismissStartDatePicker() }) { Text("取消") } }
        ) {
            DatePicker(state = startDatePickerState) // Use startDatePickerState
        }
    }
    if (uiState.showStartTimePicker) {
        TimePickerDialog(
            onDismissRequest = { viewModel.dismissStartTimePicker() },
            confirmButton = {
                TextButton(onClick = {
                    // Use startTimePickerState
                    viewModel.onStartTimeSelected(
                        LocalTime(
                            startTimePickerState.hour,
                            startTimePickerState.minute
                        )
                    )
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { viewModel.dismissStartTimePicker() }) { Text("取消") } }
        ) {
            TimePicker(state = startTimePickerState) // Use startTimePickerState
        }
    }
    // --- End Date Picker Dialog ---
    if (uiState.showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.dismissEndDatePicker() },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let { // Use endDatePickerState
                        viewModel.onEndDateSelected(
                            Instant.fromEpochMilliseconds(it)
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date
                        )
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { viewModel.dismissEndDatePicker() }) { Text("取消") } }
        ) {
            DatePicker(state = endDatePickerState) // Use endDatePickerState
        }
    }
    // --- End Time Picker Dialog ---
    if (uiState.showEndTimePicker) {
        TimePickerDialog(
            onDismissRequest = { viewModel.dismissEndTimePicker() },
            confirmButton = {
                TextButton(onClick = {
                    // Use endTimePickerState
                    viewModel.onEndTimeSelected(
                        LocalTime(
                            endTimePickerState.hour,
                            endTimePickerState.minute
                        )
                    )
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { viewModel.dismissEndTimePicker() }) { Text("取消") } }
        ) {
            TimePicker(state = endTimePickerState) // Use endTimePickerState
        }
    }

    // --- Color Picker Dialog --- (We'll define this later)
    if (uiState.showColorPickerDialog) {
        ColorPickerDialog(
            predefinedColors = getColorListFromColorScheme(colorScheme), // Use colors from theme
            onColorSelected = { hexColor ->
                viewModel.onColorChange(hexColor) // Update ViewModel
                viewModel.dismissColorPicker()   // Close dialog
            },
            onDismissRequest = { viewModel.dismissColorPicker() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "编辑日程" else "添加日程") }, // 根据模式显示标题
                navigationIcon = {
                    IconButton(onClick = { navigationState.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveSchedule() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Show loading indicator during initialization for edit mode
        if (uiState.isInitializing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Main content Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.onTitleChange(it) },
                    label = { Text("标题*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.errorMessage?.contains("标题") == true,
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.description ?: "",
                    onValueChange = { viewModel.onDescriptionChange(it) },
                    label = { Text("描述") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp) // 多行文本框
                )

                // 新增: Location
                OutlinedTextField(
                    value = uiState.location ?: "",
                    onValueChange = { viewModel.onLocationChange(it) },
                    label = { Text("地点") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 新增: Category
                OutlinedTextField(
                    value = uiState.category ?: "",
                    onValueChange = { viewModel.onCategoryChange(it) },
                    label = { Text("分类") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // --- Replace Color TextField with Preview and Button ---
                Text("颜色", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Color Preview Box
                    val previewColor = remember(uiState.color) {
                        parseColor(
                            uiState.color ?: "#"
                        ) // Use helper to parse, default to transparent/error color if invalid
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(previewColor)
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                CircleShape
                            )
                    )
                    // Button to open picker
                    TextButton(onClick = { viewModel.showColorPicker() }) {
                        Text("选择颜色")
                    }
                }
                // --- End Color Section ---

                // 新增: Status (使用 Dropdown)
                StatusDropdown( // Extract to separate composable?
                    selectedStatus = uiState.status,
                    onStatusSelected = { viewModel.onStatusChange(it) }
                )

                // --- Start Date/Time Section ---
                Text("开始时间", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateTimeDisplayChip(
                        icon = Icons.Default.DateRange,
                        text = uiState.startDate?.formatDate() ?: "选择日期",
                        onClick = { viewModel.showStartDatePicker() }
                    )
                    DateTimeDisplayChip(
                        icon = Icons.Default.Schedule,
                        text = uiState.startTime?.formatTime() ?: "选择时间",
                        onClick = { viewModel.showStartTimePicker() }
                    )
                }

                // --- End Date/Time Section ---
                Text("结束时间", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateTimeDisplayChip(
                        icon = Icons.Default.DateRange,
                        text = uiState.endDate?.formatDate() ?: "选择日期",
                        onClick = { viewModel.showEndDatePicker() }
                    )
                    DateTimeDisplayChip(
                        icon = Icons.Default.Schedule,
                        text = uiState.endTime?.formatTime() ?: "选择时间",
                        onClick = { viewModel.showEndTimePicker() }
                    )
                }

                if (uiState.isLoading && !uiState.isInitializing) { // Show saving indicator
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

// Helper composable for displaying date/time and triggering picker
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeDisplayChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        modifier = modifier,
        selected = false, // Not selectable, just clickable
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        }
    )
}

// Helper extension functions for formatting (place in a utils file later)
fun LocalDate.formatDate(): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
    return this.toJavaLocalDate().format(formatter)
}

fun LocalTime.formatTime(): String {
    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    return this.toJavaLocalTime().format(formatter)
}

// Wrapper for TimePickerDialog as it might not be directly available in older Material3 versions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    // Basic dialog implementation, ideally use androidx.compose.material3.TimePickerDialog if available
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.width(IntrinsicSize.Min), // Adjust size as needed
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = containerColor,
            tonalElevation = 6.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.padding(
                        top = 24.dp,
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 12.dp
                    )
                ) {
                    content() // Place the TimePicker here
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

// 新增: Status Dropdown Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(
    selectedStatus: com.example.todoschedule.data.database.converter.ScheduleStatus,
    onStatusSelected: (com.example.todoschedule.data.database.converter.ScheduleStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val items = com.example.todoschedule.data.database.converter.ScheduleStatus.entries.toList()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedStatus.name, // Display the name of the enum
            onValueChange = {}, // Not editable directly
            readOnly = true,
            label = { Text("状态") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth() // Important: menuAnchor
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.name) },
                    onClick = {
                        onStatusSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Helper function to safely parse a Hex color string.
 * Returns the parsed Color or a default color (e.g., transparent or gray) if parsing fails.
 */
fun parseColor(hexString: String?, defaultColor: Color = Color.Transparent): Color {
    if (hexString.isNullOrBlank()) return defaultColor
    return try {
        // Ensure the string starts with # and handle potential alpha
        val cleanHexString = if (hexString.startsWith("#")) hexString else "#$hexString"
        val colorInt = android.graphics.Color.parseColor(cleanHexString)
        Color(colorInt)
    } catch (e: IllegalArgumentException) {
        // Log.w("ColorParse", "Invalid color string: $hexString", e) // Optional logging
        defaultColor // Return default on error
    }
}

// Placeholder for ColorPickerDialog (to be implemented next)
@Composable
fun ColorPickerDialog(
    predefinedColors: List<Color>,
    onColorSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("选择颜色") },
        text = {
            // Use LazyVerticalGrid to display color swatches
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp), // Adjust minSize as needed
                modifier = Modifier
                    .heightIn(max = 300.dp) // Limit height
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(predefinedColors) { colorHex ->
                    val color = remember(colorHex) {
                        parseColor(
                            colorHex.toString(),
                            Color.Gray
                        )
                    } // Default to gray on error
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                CircleShape
                            )
                            .clickable { onColorSelected(colorHex.toString()) } // Select color on click
                    )
                }
            }
        },
        confirmButton = { // Using confirmButton slot for the dismiss action text
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        },
        dismissButton = null // No explicit dismiss button needed if clicking outside closes it
    )
} 