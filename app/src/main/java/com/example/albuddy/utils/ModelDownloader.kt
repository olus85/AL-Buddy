package com.example.albuddy.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import com.example.albuddy.di.DefaultOkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    @DefaultOkHttpClient private val okHttpClient: OkHttpClient
) {
    fun downloadAndUnpackModel(url: String, destinationDirName: String): Flow<Int> = flow {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw RuntimeException("Failed to download model: ${response.code}")
        }

        val body = response.body ?: throw RuntimeException("Empty response body")
        val contentLength = body.contentLength()
        
        val targetDir = File(context.filesDir, destinationDirName)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        // If it's a ZIP file (like Vosk models), we unzip on the fly. If not (like Whisper .bin), we save directly.
        val isZip = url.endsWith(".zip")

        if (isZip) {
            ZipInputStream(body.byteStream()).use { zis ->
                var zipEntry = zis.nextEntry
                while (zipEntry != null) {
                    val newFile = File(targetDir, zipEntry.name)
                    if (zipEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    zipEntry = zis.nextEntry
                }
            }
            emit(100)
        } else {
            val file = File(targetDir, url.substringAfterLast("/"))
            body.byteStream().use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesCopied: Long = 0
                    var bytesRead: Int
                    var lastProgress = -1

                    while (inputStream.read(buffer).also { bytesRead = it } >= 0) {
                        outputStream.write(buffer, 0, bytesRead)
                        bytesCopied += bytesRead

                        val progress = if (contentLength > 0) {
                            ((bytesCopied.toFloat() / contentLength.toFloat()) * 100).toInt()
                        } else {
                            -1
                        }

                        if (progress != lastProgress) {
                            emit(progress)
                            lastProgress = progress
                        }
                    }
                }
            }
            emit(100)
        }
    }.flowOn(Dispatchers.IO)
}
