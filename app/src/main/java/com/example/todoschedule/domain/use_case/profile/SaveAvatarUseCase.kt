package com.example.todoschedule.domain.use_case.profile

import android.net.Uri
import com.example.todoschedule.domain.model.User
import com.example.todoschedule.domain.repository.FileRepository
import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.repository.UserRepository
import com.example.todoschedule.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SaveAvatarUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val fileRepository: FileRepository,
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(avatarUri: Uri): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val currentUserId = sessionRepository.currentUserIdFlow.firstOrNull()
            if (currentUserId == null) {
                emit(Resource.Error("User not logged in."))
                return@flow
            }

            val currentUser = userRepository.getUserById(currentUserId.toInt())
            if (currentUser == null) {
                emit(Resource.Error("User not found."))
                return@flow
            }

            // Save the new avatar image and get its path
            when (val saveResult = fileRepository.saveImageFromUri(avatarUri, "avatar_")) {
                is Resource.Success -> {
                    val newAvatarPath = saveResult.data
                    if (newAvatarPath != null) {
                        // Delete old avatar if it exists and is an internal file
                        currentUser.avatar?.let { oldAvatarPath ->
                            if (oldAvatarPath.startsWith(userRepository.getInternalFilesDir().path)) {
                                fileRepository.deleteFile(oldAvatarPath) // Assumes FileRepository has deleteFile
                            }
                        }

                        val updatedUser = currentUser.copy(avatar = newAvatarPath)
                        userRepository.updateUser(updatedUser)
                        emit(Resource.Success(updatedUser))
                    } else {
                        emit(Resource.Error("Failed to get new avatar path after saving."))
                    }
                }
                is Resource.Error -> {
                    emit(Resource.Error(saveResult.message ?: "Failed to save avatar image."))
                }
                is Resource.Loading -> {
                    // Should not happen here as saveImageFromUri is suspend
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occurred while saving avatar."))
        }
    }
}

// Note: FileRepository will need a deleteFile(filePath: String): Boolean method
// And UserRepository might need a getInternalFilesDir(): File method or similar
// to check if the old avatar path is an internal file managed by the app.
 