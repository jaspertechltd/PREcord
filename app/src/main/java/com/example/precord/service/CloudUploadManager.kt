package com.example.precord.service

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.precord.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages automatic cloud backup of captured audio files.
 * Uses Android's Storage Access Framework (SAF) so the user can pick
 * any folder — including Google Drive, Dropbox, or local storage —
 * as the backup destination. No API keys or SDKs required.
 */
object CloudUploadManager {

    /**
     * Upload a file to the configured cloud/backup folder.
     *
     * @param context Application context
     * @param sourceFile The audio file to upload
     * @return true if upload succeeded, false otherwise
     */
    suspend fun uploadFile(context: Context, sourceFile: File): Boolean = withContext(Dispatchers.IO) {
        val prefs = PreferencesManager(context)
        val uriString = prefs.cloudUploadUri ?: return@withContext false
        val destUri = Uri.parse(uriString)

        try {
            val destFolder = DocumentFile.fromTreeUri(context, destUri) ?: return@withContext false
            if (!destFolder.exists()) return@withContext false

            // Determine MIME type
            val mimeType = when (sourceFile.extension.lowercase()) {
                "wav" -> "audio/wav"
                "mp3" -> "audio/mpeg"
                "aiff" -> "audio/aiff"
                "ogg" -> "audio/ogg"
                "flac" -> "audio/flac"
                else -> "audio/*"
            }

            // Check if file already exists in destination
            val existing = destFolder.findFile(sourceFile.name)
            if (existing != null && existing.exists()) {
                return@withContext true // Already uploaded
            }

            // Create the file in the destination folder
            val destFile = destFolder.createFile(mimeType, sourceFile.nameWithoutExtension)
                ?: return@withContext false

            // Copy content
            val resolver = context.contentResolver
            resolver.openOutputStream(destFile.uri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream, bufferSize = 8192)
                }
            } ?: return@withContext false

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Check if cloud upload is configured and enabled.
     */
    fun isConfigured(context: Context): Boolean {
        val prefs = PreferencesManager(context)
        return prefs.cloudUploadEnabled && prefs.cloudUploadUri != null
    }

    /**
     * Verify that the configured destination folder still exists and is writable.
     */
    fun isDestinationValid(context: Context): Boolean {
        val prefs = PreferencesManager(context)
        val uriString = prefs.cloudUploadUri ?: return false
        return try {
            val uri = Uri.parse(uriString)
            val folder = DocumentFile.fromTreeUri(context, uri)
            folder?.exists() == true && folder.canWrite()
        } catch (_: Exception) {
            false
        }
    }
}
