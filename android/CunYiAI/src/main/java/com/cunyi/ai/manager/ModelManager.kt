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

    // 模型类型枚举
    enum class ModelType(val url: String, val filename: String, val size: Long, val displayName: String, val description: String) {
        GEMMA(
            "https://hf-mirror.com/unsloth/gemma-4-E2B-it-GGUF/resolve/main/gemma-4-E2B-it-Q4_K_M.gguf",
            "gemma-4-E2B-it-Q4_K_M.gguf",
            3L * 1024 * 1024 * 1024 + (1024 * 1024 * 1024 / 10),
            "Gemma 4 E2B",
            "约3.1GB，适合一般手机"
        ),
        BAICHUAN(
            "https://www.modelscope.cn/models/baichuan-inc/Baichuan-M3-235B-Q4_K_M-GGUF/resolve/master/baichuan-m3-235b-q4_k_m-00010-of-00010.gguf",
            "baichuan-m3-235b-q4_k_m.gguf",
            140L * 1024 * 1024 * 1024,
            "Baichuan M3 235B",
            "约140GB，适合高配手机(8G+)"
        ),
        MEDGEMMA(
            "https://www.modelscope.cn/models/unsloth/medgemma-1.5-4b-it-GGUF/resolve/master/medgemma-1.5-4b-it-Q4_K_M.gguf",
            "medgemma-1.5-4b-it-Q4_K_M.gguf",
            2700L * 1024 * 1024,
            "MedGemma 1.5 4B",
            "约2.7GB，医疗领域专用"
        )
    }

    companion object {
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
        return getModelPath(ModelType.GEMMA)
    }
    
    /**
     * 获取指定类型模型的路径
     */
    fun getModelPath(modelType: ModelType): File {
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return if (externalDir != null) {
            File(externalDir, "models/${modelType.filename}")
        } else {
            File(context.filesDir, "models/${modelType.filename}")
        }
    }
    
    /**
     * 获取所有可能的模型文件路径
     */
    private fun getAllModelPaths(): List<File> {
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val baseDir = if (externalDir != null) File(externalDir, "models") else File(context.filesDir, "models")
        return ModelType.values().map { File(baseDir, it.filename) }
    }

    /**
     * 检查指定模型是否已下载
     */
    fun isModelDownloaded(modelType: ModelType): Boolean {
        val modelFile = getModelPath(modelType)
        return modelFile.exists() && modelFile.length() > 100 * 1024 * 1024
    }

    /**
     * 检查模型是否已下载（任一模型）
     */
    fun isModelDownloaded(): Boolean {
        return ModelType.values().any { isModelDownloaded(it) }
    }

    /**
     * 获取已下载模型的类型
     */
    fun getDownloadedModelType(): ModelType? {
        return ModelType.values().find { isModelDownloaded(it) }
    }

    /**
     * 获取已下载模型大小
     */
    fun getDownloadedModelSize(): Long {
        return ModelType.values().filter { isModelDownloaded(it) }.maxOfOrNull { getModelPath(it).length() } ?: 0L
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
     * 下载模型（支持指定模型类型）
     * @param modelType 指定要下载的模型类型，null时按默认顺序尝试所有模型
     */
    suspend fun downloadModel(modelType: ModelType? = null) = withContext(Dispatchers.IO) {
        // 确定要下载的URL列表
        val urls: List<Triple<String, String, Long>>
        
        if (modelType != null) {
            // 指定了具体模型，只下载该模型
            urls = listOf(Triple(modelType.url, modelType.filename, modelType.size))
        } else {
            // 未指定，按默认顺序尝试所有模型
            urls = ModelType.values().map { Triple(it.url, it.filename, it.size) }
        }
        
        var lastError = ""
        
        for ((url, filename, expectedSize) in urls) {
            try {
                _downloadState.value = DownloadState.Connecting
                
                // 检查指定模型是否已存在
                val targetModelType = modelType ?: ModelType.values().find { it.filename == filename }
                if (targetModelType != null && isModelDownloaded(targetModelType)) {
                    _downloadState.value = DownloadState.ModelExists
                    return@withContext
                }

                // 使用对应文件名创建文件
                val baseDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                    ?: context.filesDir
                val modelFile = File(File(baseDir, "models"), filename)
                modelFile.parentFile?.mkdirs()

                // 获取文件大小
                val headRequest = Request.Builder().url(url).head().build()
                val headResponse = httpClient.newCall(headRequest).execute()
                val contentLength = headResponse.header("content-length")?.toLongOrNull() ?: expectedSize
                headResponse.close()

                // 开始下载
                val downloadResponse = httpClient.newCall(Request.Builder().url(url).build()).execute()
                if (!downloadResponse.isSuccessful && downloadResponse.code !in 200..299) {
                    downloadResponse.close()
                    lastError = "HTTP ${downloadResponse.code}"
                    continue // 尝试下一个URL
                }

                val body = downloadResponse.body
                if (body == null) {
                    downloadResponse.close()
                    lastError = "服务器无响应"
                    continue
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
                                modelFile.delete()
                                return@withContext
                            }

                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

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
                val minSize = 100L * 1024 * 1024 // 最小100MB
                if (fileSize > minSize) {
                    _downloadState.value = DownloadState.Success
                    return@withContext
                } else {
                    modelFile.delete()
                    lastError = "文件不完整 (${formatSize(fileSize)})"
                }
                
            } catch (e: Exception) {
                lastError = e.message ?: "未知错误"
                continue // 尝试备用URL
            }
        }
        
        // 所有URL都失败
        val message = when {
            lastError.contains("timeout", ignoreCase = true) -> "连接超时，请检查网络"
            lastError.contains("network", ignoreCase = true) -> "网络不可用"
            else -> "下载失败: $lastError"
        }
        _downloadState.value = DownloadState.Error(message)
    }

    /**
     * 删除已下载的模型
     */
    fun deleteModel(): Boolean {
        // 删除所有可能的模型文件
        var deleted = false
        ModelType.values().forEach { modelType ->
            val modelFile = getModelPath(modelType)
            if (modelFile.exists()) {
                deleted = modelFile.delete() || deleted
            }
        }
        return deleted
    }
}
