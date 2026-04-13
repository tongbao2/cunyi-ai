package com.cunyi.ai.manager

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

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
    }

    // 下载状态
    sealed class DownloadState {
        object Idle : DownloadState()
        object Checking : DownloadState()
        data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
        data class Error(val message: String) : DownloadState()
        object Success : DownloadState()
        object ModelExists : DownloadState()
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    /**
     * 获取模型文件路径
     */
    fun getModelPath(): File {
        // 优先使用外部存储，否则使用应用私有目录
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
        return modelFile.exists() && modelFile.length() > 100 * 1024 * 1024 // 至少100MB
    }

    /**
     * 获取模型大小（如果存在）
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
     * 下载模型
     */
    suspend fun downloadModel() = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = DownloadState.Checking

            // 检查是否已存在
            if (isModelDownloaded()) {
                _downloadState.value = DownloadState.ModelExists
                return@withContext
            }

            val modelFile = getModelPath()
            modelFile.parentFile?.mkdirs()

            // 开始下载
            val url = URL(MODEL_URL)
            val connection = url.openConnection()
            connection.connect()

            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L

            connection.getInputStream().use { input ->
                FileOutputStream(modelFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // 更新进度（每1MB更新一次）
                        if (downloadedBytes % (1024 * 1024) == 0L) {
                            val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                            _downloadState.value = DownloadState.Downloading(progress, downloadedBytes, totalBytes)
                        }
                    }
                }
            }

            // 验证下载
            if (modelFile.length() > 100 * 1024 * 1024) {
                _downloadState.value = DownloadState.Success
            } else {
                modelFile.delete()
                _downloadState.value = DownloadState.Error("下载文件不完整")
            }

        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error(e.message ?: "下载失败")
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
}
