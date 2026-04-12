package com.cunyi.ai

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var chatInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var statusText: TextView
    
    private val modelUrl = "https://hf-mirror.com/unsloth/gemma-4-E2B-it-GGUF/resolve/main/gemma-4-E2B-it-Q4_K_M.gguf"
    private var modelFile: File? = null
    private var isModelLoaded = false
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        checkAndDownloadModel()
    }

    private fun initViews() {
        chatInput = findViewById(R.id.chat_input)
        sendButton = findViewById(R.id.send_button)
        statusText = findViewById(R.id.status_text)
        
        sendButton.setOnClickListener {
            val message = chatInput.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
            }
        }
        
        // 首次运行自动下载模型
        if (!isModelInstalled()) {
            statusText.text = getString(R.string.model_downloading)
            downloadModel(modelUrl)
        }
    }

    private fun checkAndDownloadModel() {
        val modelDir = File(filesDir, "models")
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        
        val modelFileName = "gemma-4-E2B-it-Q4_K_M.gguf"
        modelFile = File(modelDir, modelFileName)
        
        if (!modelFile!!.exists()) {
            statusText.text = getString(R.string.model_downloading)
            downloadModel(modelUrl)
        } else {
            isModelLoaded = true
            statusText.text = getString(R.string.ai_ready)
        }
    }

    private fun isModelInstalled(): Boolean {
        val modelDir = File(filesDir, "models")
        val modelFile = File(modelDir, "gemma-4-E2B-it-Q4_K_M.gguf")
        return modelFile.exists()
    }

    private fun downloadModel(url: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(url)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        response.body?.let { body ->
                            val totalBytes = body.contentLength()
                            var downloadedBytes = 0L
                            
                            FileOutputStream(modelFile).use { output ->
                                body.byteStream().use { input ->
                                    val buffer = ByteArray(8192)
                                    var bytesRead: Int
                                    while (input.read(buffer).also { bytesRead = it } != -1) {
                                        output.write(buffer, 0, bytesRead)
                                        downloadedBytes += bytesRead
                                        
                                        val progress = if (totalBytes > 0) {
                                            ((downloadedBytes * 100) / totalBytes).toInt()
                                        } else 0
                                        
                                        withContext(Dispatchers.Main) {
                                            statusText.text = "下载中: $progress%"
                                        }
                                    }
                                }
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            isModelLoaded = true
                            statusText.text = getString(R.string.model_downloaded)
                            Toast.makeText(this@MainActivity, R.string.model_downloaded, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            statusText.text = getString(R.string.download_failed)
                            Toast.makeText(this@MainActivity, R.string.download_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = getString(R.string.download_failed)
                    Toast.makeText(this@MainActivity, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendMessage(message: String) {
        // 在这里添加实际的 AI 推理逻辑
        // 由于是离线模式，需要集成 llama.cpp 或类似的本地推理引擎
        
        chatInput.setText("")
        
        // TODO: 实现本地模型推理
        // 这需要集成 llama.cpp 的 Android 绑定库
        
        Toast.makeText(this, "消息已发送: $message", Toast.LENGTH_SHORT).show()
    }
}