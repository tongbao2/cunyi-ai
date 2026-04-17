package com.cunyi.doctor.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ModelDownloadManager {

    private const val TAG = "ModelDownload"
    const val DEFAULT_BUFFER_SIZE = 8192

    /**
     * Downloads a file from [url] to [context.filesDir]/[fileName].
     * Reports progress via [onProgress] (0-100, message).
     */
    suspend fun download(
        context: Context,
        url: String,
        fileName: String,
        onProgress: (Int, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outFile = File(context.filesDir, fileName)
            if (outFile.exists()) {
                onProgress(100, "文件已存在")
                return@withContext Result.success(outFile)
            }

            Log.i(TAG, "Starting download: $url")
            onProgress(0, "正在连接下载服务器...")

            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 120_000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; CunYiDoctor/1.0)")
            conn.connect()

            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return@withContext Result.failure(
                    Exception("服务器返回错误: HTTP $responseCode")
                )
            }

            val contentLength = conn.contentLengthLong
            onProgress(1, "开始下载模型文件 (${formatSize(contentLength)})...")

            conn.inputStream.use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        if (contentLength > 0) {
                            val pct = ((totalBytesRead * 100) / contentLength).toInt().coerceIn(1, 99)
                            onProgress(pct, "下载中 ${formatSize(totalBytesRead)} / ${formatSize(contentLength)}")
                        }
                    }
                    output.flush()
                }
            }

            conn.disconnect()
            Log.i(TAG, "Download complete: ${outFile.absolutePath} (${outFile.length()} bytes)")
            onProgress(100, "下载完成！")
            Result.success(outFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            // Clean up partial file
            val partial = File(context.filesDir, fileName)
            if (partial.exists()) partial.delete()
            onProgress(0, "下载失败: ${e.message}")
            Result.failure(e)
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 0 -> "未知"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }
}
