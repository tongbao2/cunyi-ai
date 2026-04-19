package com.cunyi.doctor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cunyi.doctor.llm.LlamaEngine
import com.cunyi.doctor.llm.ModelConfig
import com.cunyi.doctor.utils.ImageTextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = LlamaEngine.getInstance(application)

    val modelLoadState = engine.modelLoadState
    val generationState = engine.generationState

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _currentResponse = MutableStateFlow("")
    val currentResponse: StateFlow<String> = _currentResponse

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // 文字识别状态
    private val _isRecognizing = MutableStateFlow(false)
    val isRecognizing: StateFlow<Boolean> = _isRecognizing

    fun loadModel(model: ModelConfig? = null, onProgress: (Int, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            if (model != null) {
                engine.selectModel(model)
            }
            engine.loadModel(onProgress)
            _isLoading.value = false
        }
    }

    fun sendMessage(text: String, imagePath: String? = null) {
        if (text.isBlank() && imagePath == null) return
        if (!engine.isModelLoaded) return

        viewModelScope.launch {
            // 如果有图片，先用 ML Kit 识别文字
            var recognizedText: String? = null
            if (imagePath != null) {
                _isRecognizing.value = true
                recognizedText = withContext(Dispatchers.IO) {
                    ImageTextRecognizer.recognizeFromPath(imagePath)
                }
                _isRecognizing.value = false
            }
            
            // 创建用户消息
            val userMsg = if (imagePath != null) {
                val caption = recognizedText?.let { "[图片文字: $it]" } ?: "[图片]"
                ChatMessage(
                    content = if (text.isBlank()) caption else "$caption\n$text",
                    isUser = true, 
                    imagePath = imagePath
                )
            } else {
                ChatMessage(text, isUser = true)
            }
            
            _chatMessages.value = _chatMessages.value + userMsg
            
            // 构建 prompt
            val promptText = buildPrompt(text, imagePath, recognizedText)
            
            _currentResponse.value = ""
            _isLoading.value = true

            engine.generate(
                prompt = promptText,
                onToken = { token ->
                    if (token != "[DONE]" && token != "[ERROR: 模型未加载]" &&
                        !token.startsWith("[ERROR")) {
                        _currentResponse.value += token
                    }
                },
                onDone = { fullText ->
                    val botMsg = ChatMessage(fullText, isUser = false)
                    _chatMessages.value = _chatMessages.value + botMsg
                    _isLoading.value = false
                    _currentResponse.value = ""
                },
                onError = { err ->
                    _currentResponse.value = "[出错了] $err"
                    _isLoading.value = false
                }
            )
        }
    }
    
    // 构建发送给 LLM 的 prompt
    private fun buildPrompt(userText: String, imagePath: String?, recognizedText: String?): String {
        return if (imagePath != null) {
            // 有图片的情况
            if (recognizedText != null && recognizedText.isNotBlank()) {
                // 识别到文字
                "[图片中识别到以下文字: $recognizedText]\n\n用户问题: $userText"
            } else {
                // 未识别到文字
                "[用户发送了一张图片]\n$userText"
            }
        } else {
            // 纯文字
            userText
        }
    }

    fun stopGeneration() {
        engine.stop()
        _isLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }

    // 支持图片的消息
    data class ImageMessage(
        val imagePath: String, 
        val isUser: Boolean = true,
        val caption: String = ""
    )

    data class ChatMessage(
        val content: String, 
        val isUser: Boolean,
        val imagePath: String? = null  // 可选：图片路径
    )
}
