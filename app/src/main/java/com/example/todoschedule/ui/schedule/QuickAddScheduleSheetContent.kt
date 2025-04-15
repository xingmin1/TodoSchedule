package com.example.todoschedule.ui.schedule

import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.ui.ordinaryschedule.TimePickerDialog
import com.example.todoschedule.ui.ordinaryschedule.formatDate
import com.example.todoschedule.ui.ordinaryschedule.formatTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Refactored Quick Add BottomSheet Content using ViewModel.
 *
 * @param onDismiss Request to close the BottomSheet.
 * @param modifier Modifier for the content root.
 * @param viewModel ViewModel instance for managing state and actions.
 */
@OptIn(ExperimentalMaterial3Api::class) // Required for Material 3 components
@Composable
fun QuickAddScheduleSheetContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickAddScheduleViewModel = hiltViewModel() // Inject ViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // --- Focus Management --- (Simplified)
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // 每次 BottomSheet 打开时，确保状态被重置
    LaunchedEffect(Unit) {
        viewModel.resetState()
        delay(100) // Give UI time to settle
        focusRequester.requestFocus()
    }

    // --- Error Handling --- (Using Toast for simplicity)
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeErrorMessage() // Reset error message
        }
    }

    // --- Date & Time Picker States --- (Managed by ViewModel, but need Composable state holders)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds(),
    )
    val startTimePickerState = rememberTimePickerState(is24Hour = true)
    val endTimePickerState = rememberTimePickerState(is24Hour = true)

    // Transparent TextField Colors
    val transparentTextFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .imePadding(), // Handles keyboard automatically
        verticalArrangement = Arrangement.spacedBy(12.dp) // Slightly more space
    ) {
        // --- 1. Title Input --- (Bound to ViewModel state)
        BasicTextField(
            value = uiState.title,
            onValueChange = { viewModel.onTitleChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize
            ),
            singleLine = true,
            interactionSource = remember { MutableInteractionSource() },
            enabled = !uiState.isLoading, // Disable when loading
            visualTransformation = VisualTransformation.None,
            decorationBox = { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = uiState.title,
                    innerTextField = innerTextField,
                    enabled = !uiState.isLoading,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = remember { MutableInteractionSource() },
                    placeholder = { if (uiState.title.isEmpty()) Text("记些重要的事情吧~") },
                    contentPadding = PaddingValues(0.dp),
                    colors = transparentTextFieldColors
                )
            }
        )

        // --- 2. Description Input --- (Bound to ViewModel state)
        BasicTextField(
            value = uiState.description ?: "",
            onValueChange = { viewModel.onDescriptionChange(it) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = MaterialTheme.typography.bodyMedium.fontSize
            ),
            singleLine = false,
            interactionSource = remember { MutableInteractionSource() },
            enabled = !uiState.isLoading,
            visualTransformation = VisualTransformation.None,
            decorationBox = { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = uiState.description ?: "",
                    innerTextField = innerTextField,
                    enabled = !uiState.isLoading,
                    singleLine = false,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = remember { MutableInteractionSource() },
                    placeholder = {
                        if (uiState.description.isNullOrEmpty()) Text(
                            "描述",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    },
                    contentPadding = PaddingValues(0.dp),
                    colors = transparentTextFieldColors
                )
            }
        )

        // --- 3. Date and Time Buttons --- (Simplified)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // a) Date Picker Button
            Button(
                onClick = { viewModel.showDatePicker(true) },
                shape = RoundedCornerShape(percent = 50),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                enabled = !uiState.isLoading
            ) {
                Icon(
                    Icons.Default.CalendarMonth, // Use Calendar icon
                    contentDescription = "选择日期",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = uiState.selectedDate.formatDate(), // Format date from state
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // b) Time Picker Button
            Button(
                onClick = {
                    focusManager.clearFocus() // Hide keyboard before showing picker
                    viewModel.showStartTimePicker(true)
                },
                shape = RoundedCornerShape(percent = 50),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                enabled = !uiState.isLoading
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = "选择时间",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = when {
                        uiState.startTime != null && uiState.endTime != null ->
                            "${uiState.startTime!!.formatTime()} - ${uiState.endTime!!.formatTime()}"

                        uiState.startTime != null -> uiState.startTime!!.formatTime()
                        else -> "时间"
                    },
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // c) Save Button
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                IconButton(
                    onClick = { viewModel.saveSchedule(onDismiss) }, // Call VM save
                    enabled = uiState.title.isNotBlank() && !uiState.isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "保存",
                        tint = if (uiState.title.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f
                        )
                    )
                }
            }
        } // End Row
    } // End Column

    // --- Date Picker Dialog ---
    if (uiState.showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.showDatePicker(false) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.onDateSelected(
                            Instant.fromEpochMilliseconds(it)
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date
                        )
                    } ?: viewModel.showDatePicker(false) // Dismiss if null
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { viewModel.showDatePicker(false) }) { Text("取消") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- Start Time Picker Dialog ---
    if (uiState.showStartTimePicker) {
        // Use the TimePickerDialog from ordinaryschedule package
        TimePickerDialog(
            onDismissRequest = {
                viewModel.showStartTimePicker(false)
                // Request focus back? Maybe not needed if end picker opens
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onStartTimeSelected(
                            kotlinx.datetime.LocalTime(
                                startTimePickerState.hour,
                                startTimePickerState.minute
                            )
                        )
                        // VM handles opening end time picker
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showStartTimePicker(false) }) { Text("取消") }
            }
        ) {
            // Content for TimePickerDialog (the TimePicker itself)
            TimePicker(state = startTimePickerState)
        }
    }

    // --- End Time Picker Dialog ---
    if (uiState.showEndTimePicker) {
        // Pre-populate end time picker state if start time is available
        LaunchedEffect(uiState.startTime) {
            uiState.startTime?.let {
                val suggestedEndHour = (it.hour + 1) % 24
                endTimePickerState.hour = suggestedEndHour
                endTimePickerState.minute = it.minute
            }
        }

        TimePickerDialog(
            onDismissRequest = { viewModel.showEndTimePicker(false) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEndTimeSelected(
                            kotlinx.datetime.LocalTime(
                                endTimePickerState.hour,
                                endTimePickerState.minute
                            )
                        )
                        scope.launch { // Request focus back after closing
                            delay(100)
                            focusRequester.requestFocus()
                        }
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showEndTimePicker(false) }) { Text("取消") }
            }
        ) {
            TimePicker(state = endTimePickerState)
        }
    }
}

// --- Preview --- (Updated)
@Preview(showBackground = true)
@Composable
private fun QuickAddScheduleSheetContentPreview() {
    MaterialTheme {
        // Preview needs a way to provide a dummy ViewModel or handle null state
        // For simplicity, we'll just call it directly, it might crash in preview
        // A better approach involves Hilt preview setup or fake ViewModel
        QuickAddScheduleSheetContent(
            onDismiss = {}
            // Cannot provide Hilt ViewModel in Preview easily
        )
    }
}
