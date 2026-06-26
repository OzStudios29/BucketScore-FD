package com.example.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageStorageManager {

    /**
     * Creates a temporary file in the cache directory for storing camera capture photos.
     */
    fun createTempImageFile(context: Context): File {
        val storageDir = context.cacheDir
        return File.createTempFile(
            "JPEG_${UUID.randomUUID()}_",
            ".jpg",
            storageDir
        )
    }

    /**
     * Obtains a content URI for a file using the FileProvider.
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Copies an image from a source Uri (Gallery or Camera temp) to the app's internal "images" directory.
     * Then, if Firebase is initialized, attempts to upload it to Firebase Storage.
     * Returns either the Firebase Storage URL or the local file Uri string as fallback.
     */
    suspend fun saveAndUploadImage(
        context: Context,
        sourceUri: Uri,
        folderName: String
    ): String = withContext(Dispatchers.IO) {
        try {
            // 1. Copy to internal storage
            val imagesDir = File(context.filesDir, "images/$folderName").apply {
                if (!exists()) mkdirs()
            }
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val localFile = File(imagesDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(localFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val localUriStr = Uri.fromFile(localFile).toString()

            // 2. Try Firebase Storage upload if initialized
            if (isFirebaseInitialized(context)) {
                try {
                    val storageRef = Firebase.storage.reference
                        .child("bucketscore/$folderName/$fileName")
                    
                    storageRef.putFile(Uri.fromFile(localFile)).await()
                    val downloadUrl = storageRef.downloadUrl.await()
                    return@withContext downloadUrl.toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fall back to local file path
                    return@withContext localUriStr
                }
            }

            return@withContext localUriStr
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext sourceUri.toString()
        }
    }

    private fun isFirebaseInitialized(context: Context): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
