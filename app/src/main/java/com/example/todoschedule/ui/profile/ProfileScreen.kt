package com.example.todoschedule.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.todoschedule.R
import com.example.todoschedule.domain.model.User
import com.example.todoschedule.ui.profile.model.EditField
import com.example.todoschedule.ui.profile.model.ProfileEvent
import com.example.todoschedule.ui.profile.model.ProfileUiState
import com.example.todoschedule.ui.theme.TodoScheduleTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Dummy User for Preview
val previewUser = User(
    id = 1,
    username = "张三",
    email = "zhangsan@example.com",
    phoneNumber = "13800138000",
    avatar = null, // Will use placeholder
    createdAt = Instant.parse("2023-01-15T10:30:00Z"),
    lastOpen = Instant.parse("2024-05-10T18:45:00Z")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    paddingValues: PaddingValues,
    viewModel: ProfileViewModel = hiltViewModel(),
    onLogoutSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.handleEvent(ProfileEvent.UpdateAvatar(uri))
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.handleEvent(ProfileEvent.LoadProfile)
    }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogoutSuccess()
        }
    }

    LaunchedEffect(snackbarHostState, viewModel) {
        viewModel.uiState.collectLatest { state ->
            state.successMessage?.let {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
                viewModel.handleEvent(ProfileEvent.ClearMessages)
            }
            state.error?.let {
                if (state.editingField == null && !state.isSaving) {
                    snackbarHostState.showSnackbar(
                        message = it,
                        duration = SnackbarDuration.Long,
                        actionLabel = "Dismiss"
                    )
                    viewModel.handleEvent(ProfileEvent.ClearMessages)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.profile_title)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val showLoading = (uiState.isLoading && uiState.user == null && uiState.editingField == null && !uiState.isSaving) || 
                              (uiState.isSaving && uiState.editingField == null) 

            if (showLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (uiState.error != null && uiState.user == null && uiState.editingField == null) {
                Text(
                    text = stringResource(id = R.string.profile_load_error, uiState.error!!),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (uiState.user != null) {
                ProfileHeader(
                    user = uiState.user!!,
                    onEditAvatar = {
                        pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onEditUsername = { viewModel.handleEvent(ProfileEvent.StartEditField(EditField.USERNAME)) }
                )
                Spacer(modifier = Modifier.height(24.dp))
                ProfileInfoList(
                    user = uiState.user!!,
                    onEditField = { field -> viewModel.handleEvent(ProfileEvent.StartEditField(field)) }
                )
                Spacer(modifier = Modifier.height(32.dp))
                LogoutButton(
                    onLogout = { viewModel.handleEvent(ProfileEvent.Logout) },
                    isLoading = uiState.isLoading || uiState.isSaving
                )
            } else if (!uiState.isLoading && !uiState.isSaving) {
                Text(stringResource(id = R.string.profile_no_user_info), modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            uiState.editingField?.let { field ->
                if (field != EditField.AVATAR) {
                    val currentUser = uiState.user
                    if (currentUser != null) {
                        val currentValue = when (field) {
                            EditField.USERNAME -> currentUser.username
                            EditField.EMAIL -> currentUser.email ?: ""
                            EditField.PHONE -> currentUser.phoneNumber ?: ""
                            EditField.AVATAR -> TODO()
                        }
                        EditTextFieldDialog(
                            editingField = field,
                            currentValue = currentValue,
                            onSave = { editedField, newValue ->
                                viewModel.handleEvent(ProfileEvent.SaveEditedField(editedField, newValue))
                            },
                            onDismiss = { viewModel.handleEvent(ProfileEvent.CancelEdit) },
                            isSaving = uiState.isSaving,
                            errorMessage = uiState.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    user: User,
    onEditAvatar: () -> Unit,
    onEditUsername: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Box(modifier = Modifier.clickable(onClick = onEditAvatar)) {
            AsyncImage(
                model = user.avatar ?: R.drawable.avatar_placeholder,
                contentDescription = stringResource(id = R.string.profile_avatar_desc),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.avatar_placeholder),
                error = painterResource(id = R.drawable.avatar_placeholder),
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(id = R.string.profile_edit_avatar_desc),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    .padding(6.dp)
                    .size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onEditUsername)
        ) {
            Text(
                text = user.username,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(id = R.string.profile_edit_username_desc),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ProfileInfoList(
    user: User,
    onEditField: (EditField) -> Unit
) {
    LocalContext.current
    val dateFormatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ProfileInfoItem(
                icon = Icons.Filled.AccountCircle,
                label = stringResource(id = R.string.profile_label_user_id),
                value = user.id.toString(),
                isEditable = false
            )
            HorizontalDivider()
            ProfileInfoItem(
                icon = Icons.Filled.Email,
                label = stringResource(id = R.string.profile_label_email),
                value = user.email ?: stringResource(id = R.string.profile_not_set),
                isEditable = true,
                onEditClick = { onEditField(EditField.EMAIL) }
            )
            HorizontalDivider()
            ProfileInfoItem(
                icon = Icons.Filled.Phone,
                label = stringResource(id = R.string.profile_label_phone),
                value = user.phoneNumber ?: stringResource(id = R.string.profile_not_set),
                isEditable = true,
                onEditClick = { onEditField(EditField.PHONE) }
            )
            HorizontalDivider()
            ProfileInfoItem(
                icon = Icons.Filled.CalendarToday,
                label = stringResource(id = R.string.profile_label_created_at),
                value = user.createdAt.toJavaInstant().let { dateFormatter.format(it) } ?: stringResource(id = R.string.profile_not_set),
                isEditable = false
            )
            HorizontalDivider()
            ProfileInfoItem(
                icon = Icons.Filled.History,
                label = stringResource(id = R.string.profile_label_last_opened),
                value = user.lastOpen.toJavaInstant().let { dateFormatter.format(it) } ?: stringResource(id = R.string.profile_not_set),
                isEditable = false
            )
        }
    }
}

@Composable
fun ProfileInfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    isEditable: Boolean,
    onEditClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable(enabled = isEditable && onEditClick != null, onClick = { onEditClick?.invoke() }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
        if (isEditable && onEditClick != null) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(id = R.string.profile_edit_field_desc, label),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LogoutButton(onLogout: () -> Unit, isLoading: Boolean) {
    Button(
        onClick = onLogout,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White
            )
        } else {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(id = R.string.profile_logout_button), color = Color.White)
        }
    }
}

// Preview for ProfileScreen (Light Theme)
@Preview(showBackground = true, name = "Profile Screen Light")
@Composable
fun ProfileScreenPreviewLight() {
    TodoScheduleTheme(darkTheme = false) {
        val previewState = ProfileUiState(user = previewUser, isLoading = false, error = null)
        ProfileScreenContentForPreview(uiState = previewState, paddingValues = PaddingValues(0.dp))
    }
}

// Preview for ProfileScreen (Dark Theme)
@Preview(showBackground = true, name = "Profile Screen Dark")
@Composable
fun ProfileScreenPreviewDark() {
    TodoScheduleTheme(darkTheme = true) {
        val previewState = ProfileUiState(user = previewUser, isLoading = false, error = null)
        ProfileScreenContentForPreview(uiState = previewState, paddingValues = PaddingValues(0.dp))
    }
}

// A simplified content composable for preview purposes to avoid Hilt issues.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContentForPreview(uiState: ProfileUiState, paddingValues: PaddingValues) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.profile_title)) }
            )
        },
        modifier = Modifier.padding(paddingValues)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (uiState.error != null) {
                Text(
                    text = stringResource(id = R.string.profile_load_error, uiState.error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (uiState.user != null) {
                ProfileHeader(
                    user = uiState.user,
                    onEditAvatar = { /* No-op */ },
                    onEditUsername = { /* No-op */ }
                )
                Spacer(modifier = Modifier.height(24.dp))
                ProfileInfoList(
                    user = uiState.user,
                    onEditField = { /* No-op */ }
                )
                Spacer(modifier = Modifier.height(32.dp))
                LogoutButton(
                    onLogout = { /* No-op */ },
                    isLoading = false
                )
            } else {
                Text(stringResource(id = R.string.profile_no_user_info), modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Preview(showBackground = true, name = "Profile Header Preview - Default")
@Composable
fun ProfileHeaderPreviewDefault() {
    TodoScheduleTheme {
        ProfileHeader(
            user = previewUser,
            onEditAvatar = {},
            onEditUsername = {}
        )
    }
}

@Preview(showBackground = true, name = "Profile Header Preview - With Avatar Path")
@Composable
fun ProfileHeaderPreviewWithAvatarPath() {
    val userWithAvatar = previewUser.copy(avatar = "file:///android_asset/avatar_placeholder.png") // Example asset path
    TodoScheduleTheme {
        ProfileHeader(
            user = userWithAvatar,
            onEditAvatar = {},
            onEditUsername = {}
        )
    }
}

@Preview(showBackground = true, name = "Profile Info List Preview")
@Composable
fun ProfileInfoListPreview() {
    TodoScheduleTheme {
        ProfileInfoList(
            user = previewUser,
            onEditField = {}
        )
    }
}

@Preview(showBackground = true, name = "Profile Info Item Editable Preview")
@Composable
fun ProfileInfoItemEditablePreview() {
    TodoScheduleTheme {
        ProfileInfoItem(
            icon = Icons.Filled.Email,
            label = "邮箱",
            value = "test@example.com",
            isEditable = true,
            onEditClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Profile Info Item Non-Editable Preview")
@Composable
fun ProfileInfoItemNonEditablePreview() {
    TodoScheduleTheme {
        ProfileInfoItem(
            icon = Icons.Filled.Person,
            label = "用户ID",
            value = "12345",
            isEditable = false
        )
    }
}

@Preview(showBackground = true, name = "Logout Button Preview")
@Composable
fun LogoutButtonPreview() {
    TodoScheduleTheme {
        LogoutButton(onLogout = {}, isLoading = false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Profile Screen Content Preview - Loading")
@Composable
fun ProfileScreenContentLoadingPreview() {
    TodoScheduleTheme {
        ProfileScreenContentForPreview(
            uiState = ProfileUiState(isLoading = true),
            paddingValues = PaddingValues(0.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Profile Screen Content Preview - Error")
@Composable
fun ProfileScreenContentErrorPreview() {
    TodoScheduleTheme {
        ProfileScreenContentForPreview(
            uiState = ProfileUiState(error = "Sample error message"),
            paddingValues = PaddingValues(0.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Profile Screen Content Preview - With User")
@Composable
fun ProfileScreenContentWithUserPreview() {
    TodoScheduleTheme {
        ProfileScreenContentForPreview(
            uiState = ProfileUiState(user = previewUser),
            paddingValues = PaddingValues(0.dp)
        )
    }
}

@Preview(showBackground = true, name = "Edit Text Field Dialog Preview - Username")
@Composable
fun EditTextFieldDialogUsernamePreview() {
    TodoScheduleTheme {
        EditTextFieldDialog(
            editingField = EditField.USERNAME,
            currentValue = "Old Username",
            onSave = { _, _ -> },
            onDismiss = {},
            isSaving = false,
            errorMessage = null
        )
    }
}

@Preview(showBackground = true, name = "Edit Text Field Dialog Preview - Email with Error")
@Composable
fun EditTextFieldDialogEmailErrorPreview() {
    TodoScheduleTheme {
        EditTextFieldDialog(
            editingField = EditField.EMAIL,
            currentValue = "invalidemail",
            onSave = { _, _ -> },
            onDismiss = {},
            isSaving = false,
            errorMessage = "邮箱格式不正确"
        )
    }
}

@Preview(showBackground = true, name = "Edit Text Field Dialog Preview - Saving")
@Composable
fun EditTextFieldDialogSavingPreview() {
    TodoScheduleTheme {
        EditTextFieldDialog(
            editingField = EditField.PHONE,
            currentValue = "1234567890",
            onSave = { _, _ -> },
            onDismiss = {},
            isSaving = true,
            errorMessage = null
        )
    }
}

// It's good practice to use string resources.
// Make sure these are added to your strings.xml:
// R.string.profile_title -> "个人资料"
// R.string.navigate_back -> "返回"
// R.string.profile_load_error -> "加载失败: %s"
// R.string.profile_no_user_info -> "未找到用户信息。"
// R.string.profile_avatar_desc -> "用户头像"
// R.string.profile_edit_avatar_desc -> "编辑头像"
// R.string.profile_username_not_set -> "未设置用户名"
// R.string.profile_edit_username_desc -> "编辑用户名"
// R.string.profile_label_user_id -> "用户ID"
// R.string.profile_label_email -> "邮箱"
// R.string.profile_not_set -> "未设置"
// R.string.profile_label_phone -> "手机号码"
// R.string.profile_label_created_at -> "注册时间"
// R.string.profile_label_last_opened -> "上次打开"
// R.string.profile_edit_field_desc -> "编辑 %s"
// R.string.profile_logout_button -> "退出登录"

// Add collectAsStateWithLifecycle import if not present
// import androidx.lifecycle.compose.collectAsStateWithLifecycle
// It's usually in:
// import androidx.lifecycle.compose.collectAsStateWithLifecycle // (or similar, check your dependencies)
// For this to work, you need the dependency:
// implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.0") // or newer version

// Helper for formatting dates, can remain in ProfileInfoList or be moved to a util file
// import java.time.ZoneId
// import java.time.format.DateTimeFormatter
// import java.time.format.FormatStyle
