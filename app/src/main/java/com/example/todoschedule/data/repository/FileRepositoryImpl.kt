package com.example.todoschedule.data.repository

import android.content.Context
import android.net.Uri
import com.example.todoschedule.domain.repository.FileRepository
import com.example.todoschedule.domain.utils.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileRepository {

    override suspend fun saveImageFromUri(uri: Uri, fileNamePrefix: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return@withContext Resource.Error("Failed to open input stream for URI: $uri")
            }

            // Create a directory for avatars if it doesn't exist
            val avatarsDir = File(context.filesDir, "avatars")
            if (!avatarsDir.exists()) {
                if (!avatarsDir.mkdirs()) {
                    return@withContext Resource.Error("Failed to create avatars directory.")
                }
            }

            // Determine file extension or use a default
            val extension = context.contentResolver.getType(uri)?.substringAfterLast('/') ?: "jpg"
            val fileName = "${fileNamePrefix}${UUID.randomUUID()}.$extension"
            val outputFile = File(avatarsDir, fileName)

            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()

            Resource.Success(outputFile.absolutePath)
        } catch (e: IOException) {
            e.printStackTrace()
            Resource.Error("Failed to save image: ${e.message}")
        } catch (e: SecurityException) {
            e.printStackTrace()
            Resource.Error("Security permission denied for URI: $uri - ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error("An unexpected error occurred while saving the image: ${e.message}")
        }
    }

    override suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                return@withContext file.delete()
            }
            return@withContext true // File doesn't exist, so considered deleted
        } catch (e: SecurityException) {
            e.printStackTrace()
            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
} 