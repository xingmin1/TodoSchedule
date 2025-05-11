package com.example.todoschedule.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.todoschedule.R
import com.example.todoschedule.ui.profile.model.EditField

@Composable
fun EditTextFieldDialog(
    editingField: EditField,
    currentValue: String,
    onSave: (EditField, String) -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean,
    errorMessage: String? // For validation errors specific to this dialog
) {
    var text by remember(currentValue) { mutableStateOf(currentValue) }
    var showError by remember(errorMessage) { mutableStateOf(errorMessage != null) }

    val dialogTitle = when (editingField) {
        EditField.USERNAME -> stringResource(R.string.profile_edit_username_title)
        EditField.EMAIL -> stringResource(R.string.profile_edit_email_title)
        EditField.PHONE -> stringResource(R.string.profile_edit_phone_title)
        EditField.AVATAR -> stringResource(R.string.profile_edit_avatar_title) // Assuming a text input for avatar URL for now
    }

    val keyboardType = when (editingField) {
        EditField.EMAIL -> KeyboardType.Email
        EditField.PHONE -> KeyboardType.Phone
        else -> KeyboardType.Text
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(dialogTitle) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        showError = false // Hide error when user types
                    },
                    label = { Text(stringResource(R.string.profile_edit_enter_new_value)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    isError = showError && errorMessage != null,
                    enabled = !isSaving
                )
                if (showError && errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Basic client-side check (ViewModel has more robust validation)
                    if (editingField == EditField.USERNAME && text.isBlank()) {
                        showError = true
                        // error message can be set here or rely on VM's error
                    } else {
                        onSave(editingField, text)
                    }
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// Add these to strings.xml:
// <string name="profile_edit_username_title">编辑用户名</string>
// <string name="profile_edit_email_title">编辑邮箱</string>
// <string name="profile_edit_phone_title">编辑手机号</string>
// <string name="profile_edit_avatar_title">编辑头像</string> // Or a more appropriate title
// <string name="profile_edit_enter_new_value">输入新内容</string>
// <string name="save">保存</string>
// <string name="cancel">取消</string> 