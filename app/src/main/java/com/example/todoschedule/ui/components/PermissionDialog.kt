package com.example.todoschedule.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * 通用权限请求对话框
 */
@Composable
fun PermissionDialog(
    permissionTextProvider: PermissionTextProvider,
    isPermanentlyDeclined: Boolean,
    onDismiss: () -> Unit,
    onOkClick: () -> Unit,
    onGoToAppSettingsClick: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "权限请求")
        },
        text = {
            Text(
                text = if (isPermanentlyDeclined) {
                    permissionTextProvider.permanentlyDeclinedText
                } else {
                    permissionTextProvider.explainText
                }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isPermanentlyDeclined) {
                        onGoToAppSettingsClick()
                    } else {
                        onOkClick()
                    }
                }
            ) {
                Text(
                    text = if (isPermanentlyDeclined) {
                        "前往设置"
                    } else {
                        "允许"
                    }
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(text = "取消")
            }
        }
    )
}

/**
 * 提供权限相关的文本内容
 */
interface PermissionTextProvider {
    val explainText: String
    val permanentlyDeclinedText: String
}

/**
 * 日历权限的文本提供者
 */
class CalendarPermissionTextProvider : PermissionTextProvider {
    override val explainText: String
        get() = "此功能需要访问日历权限，以便将您的任务同步到系统日历并设置提醒。"
    override val permanentlyDeclinedText: String
        get() = "您已拒绝日历权限。没有此权限，应用将无法同步任务到日历或设置提醒。\n\n请在设置中手动授予权限。"
} 