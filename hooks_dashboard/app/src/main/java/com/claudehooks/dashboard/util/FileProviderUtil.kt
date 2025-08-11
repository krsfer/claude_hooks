package com.claudehooks.dashboard.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * Utility for handling file provider operations and sharing
 */
object FileProviderUtil {
    
    /**
     * Create a content URI for sharing files
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    
    /**
     * Save file to Downloads folder and return URI
     */
    fun saveToDownloads(context: Context, filename: String, content: String, mimeType: String): Result<Uri> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(Exception("Failed to create file in Downloads"))
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                
                Result.success(uri)
            } else {
                // Use legacy external storage for older versions
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, filename)
                
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                
                // Notify media scanner
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = Uri.fromFile(file)
                context.sendBroadcast(intent)
                
                Result.success(Uri.fromFile(file))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error saving file to Downloads")
            Result.failure(e)
        }
    }
    
    /**
     * Create a temporary file for sharing
     */
    fun createTempFileForSharing(context: Context, filename: String, content: String): Result<File> {
        return try {
            val tempDir = File(context.cacheDir, "shared")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            
            val tempFile = File(tempDir, filename)
            tempFile.writeText(content)
            
            Result.success(tempFile)
        } catch (e: Exception) {
            Timber.e(e, "Error creating temp file for sharing")
            Result.failure(e)
        }
    }
    
    /**
     * Get MIME type for file extension
     */
    fun getMimeType(filename: String): String {
        return when (filename.substringAfterLast('.').lowercase()) {
            "json" -> "application/json"
            "csv" -> "text/csv"
            "txt" -> "text/plain"
            else -> "text/plain"
        }
    }
    
    /**
     * Clean up old temporary files
     */
    fun cleanupTempFiles(context: Context) {
        try {
            val tempDir = File(context.cacheDir, "shared")
            if (tempDir.exists()) {
                val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // 24 hours
                tempDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < cutoffTime) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up temp files")
        }
    }
}