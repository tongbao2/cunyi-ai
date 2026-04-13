package com.cunyi.ai.manager

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * AI模型管理器
 * 负责下载和管理离线AI模型文件
 */
class ModelManager(private val context: Context) {

    companion object {
        // HuggingFace镜像地址 - Gemma 4 E2B Q4_K_M
        const val MODEL_URL = "https://hf-mirror.com/unsloth/gemma-4-E2B-it-GGUF/resolve/main/gemma-4-E2B-it-Q4_K_M.gguf"
        const val MODEL_FILENAME = "gemma-4-E2B-it-Q4_K_M.gguf"
        const val MODEL_SIZE = 3.1 * 1024 * 1024 * 1024L // 约3.1GB
        private const val BUFFER_SIZE = 128 * 1024 // 128KB buffer for smooth progress
        private const val PROGRESS_UPDATE_THRESHOLD = 512 * 1024L // 每512KB更新一次进度
    }

    // 下载状态
    sealed class DownloadState {
        object Idle : DownloadState()
        object Connecting : DownloadState()
        data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long, val speed: String) : DownloadState()
        data class Error(val message: String) : DownloadState()
        object Success : DownloadState()
        object ModelExists : DownloadState()
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    // OkHttp client with appropriate timeouts
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // No read timeout for large files
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * 获取模型文件路径
     */
    fun getModelPath(): File {
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return if (externalDir != null) {
            File(externalDir, "models/$MODEL_FILENAME")
        } else {
            File(context.filesDir, "models/$MODEL_FILENAME")
        }
    }

    /**
     * 检查模型是否已下载
     */
    fun isModelDownloaded(): Boolean {
        val modelFile = getModelPath()
        return modelFile.exists() && modelFile.length() > 100 * 1024 * 1024
    }

    /**
     * 获取已下载模型大小
     */
    fun getDownloadedModelSize(): Long {
        val modelFile = getModelPath()
        return if (modelFile.exists()) modelFile.length() else 0L
    }

    /**
     * 检查模型状态
     */
    fun checkModelState() {
        _downloadState.value = if (isModelDownloaded()) {
            DownloadState.ModelExists
        } else {
            DownloadState.Idle
        }
    }

    /**
     * 计算下载速度
     */
    private fun calculateSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024))
            bytesPerSecond >= 1024 -> String.format("%.1f KB/s", bytesPerSecond / 1024.0)
            else -> "$bytesPerSecond B/s"
        }
    }

    /**
     * 格式化文件大小
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    /**
     * 下载模型
     */
    suspend fun downloadModel() = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = DownloadState.Connecting

            // 检查是否已存在
            if (isModelDownloaded()) {
                _downloadState.value = DownloadState.ModelExists
                return@withContext
            }

            val modelFile = getModelPath()
            modelFile.parentFile?.mkdirs()

            // 第一步：获取文件大小
            val headRequest = Request.Builder()
                .url(MODEL_URL)
                .head()
                .build()

            val headResponse = httpClient.newCall(headRequest).execute()
            if (!headResponse.isSuccessful) {
                // 如果 HEAD 失败，尝试 GET 并跟跳转
                _downloadState.value = DownloadState.Connecting
            }
            val contentLength = headResponse.header("content-length")?.toLongOrNull() ?: 0L
            headResponse.close()

            // 第二步：开始下载
            val downloadRequest = Request.Builder()
                .url(MODEL_URL)
                .build()

            val downloadResponse = httpClient.newCall(downloadRequest).execute()
            if (!downloadResponse.isSuccessful && downloadResponse.code !in 200..299) {
                val errorMsg = "下载失败: HTTP ${downloadResponse.code}"
                downloadResponse.close()
                _downloadState.value = DownloadState.Error(errorMsg)
                return@withContext
            }

            val body = downloadResponse.body
            if (body == null) {
                downloadResponse.close()
                _downloadState.value = DownloadState.Error("服务器无响应")
                return@withContext
            }

            val totalBytes = if (contentLength > 0) contentLength else body.contentLength().coerceAtLeast(0L)
            var downloadedBytes = 0L
            var lastUpdateTime = System.currentTimeMillis()
            var lastUpdateBytes = 0L
            var bytesPerSecond = 0L

            body.byteStream().use { input ->
                FileOutputStream(modelFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (!coroutineContext.isActive) {
                            // 协程被取消，清理文件
                            modelFile.delete()
                            return@withContext
                        }

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // 更新进度（每512KB或每500ms更新一次）
                        val now = System.currentTimeMillis()
                        val elapsed = now - lastUpdateTime
                        if (downloadedBytes - lastUpdateBytes >= PROGRESS_UPDATE_THRESHOLD || elapsed >= 500) {
                            if (elapsed > 0) {
                                bytesPerSecond = ((downloadedBytes - lastUpdateBytes) * 1000L) / elapsed
                            }
                            val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                            _downloadState.value = DownloadState.Downloading(
                                progress = progress.coerceIn(0f, 1f),
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                speed = calculateSpeed(bytesPerSecond)
                            )
                            lastUpdateBytes = downloadedBytes
                            lastUpdateTime = now
                        }
                    }
                }
            }
            downloadResponse.close()

            // 验证下载
            val fileSize = modelFile.length()
            if (fileSize > 100 * 1024 * 1024) {
                _downloadState.value = DownloadState.Success
            } else {
                modelFile.delete()
                _downloadState.value = DownloadState.Error("下载文件不完整（${formatSize(fileSize)}），请重试")
            }

        } catch (e: Exception) {
            val message = when {
                e.message?.contains("timeout", ignoreCase = true) == true -> "连接超时，请检查网络后重试"
                e.message?.contains("network", ignoreCase = true) == true -> "网络不可用，请检查网络连接"
                e.message?.contains("cancel", ignoreCase = true) == true -> "下载已取消"
                else -> "下载失败: ${e.message}"
            }
            _downloadState.value = DownloadState.Error(message)
        }
    }

    /**
     * 删除已下载的模型
     */
    fun deleteModel(): Boolean {
        val modelFile = getModelPath()
        return if (modelFile.exists()) {
            modelFile.delete()
        } else {
            false
        }
    }
}
