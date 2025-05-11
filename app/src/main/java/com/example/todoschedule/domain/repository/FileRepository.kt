package com.example.todoschedule.domain.repository

import android.net.Uri
import com.example.todoschedule.domain.utils.Resource

interface FileRepository {
    /**
     * Saves an image from a given content URI to the app's internal storage.
     *
     * @param uri The content URI of the image to save.
     * @param fileNamePrefix A prefix for the generated file name (e.g., "avatar_").
     * @return A Resource containing the absolute file path (String) of the saved image on success,
     *         or an error message on failure.
     */
    suspend fun saveImageFromUri(uri: Uri, fileNamePrefix: String): Resource<String>

    /**
     * Deletes a file at the given absolute path.
     *
     * @param filePath The absolute path of the file to delete.
     * @return True if deletion was successful or file did not exist, false otherwise.
     */
    suspend fun deleteFile(filePath: String): Boolean
} 