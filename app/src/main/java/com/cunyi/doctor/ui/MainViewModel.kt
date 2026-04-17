package com.cunyi.doctor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cunyi.doctor.llm.LlamaEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

    fun loadModel(onProgress: (Int, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            engine.loadModel(onProgress)
            _isLoading.value = false
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        if (!engine.isModelLoaded) return

        val userMsg = ChatMessage(text, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg
        _currentResponse.value = ""
        _isLoading.value = true

        engine.generate(
            prompt = text,
            onToken = { token ->
                if (token != "[DONE]" && token != "[ERROR: 模型未加载]" &&
                    !token.startsWith("[ERROR")) {
                    _currentResponse.value += token
                }
            },
            onDone = { fullText ->
                val botMsg = ChatMessage(fullText, isUser = false)
                _chatMessages.value = _chatMessages.value + botMsg
                // 先设置 isLoading = false，让 UI 有时间获取 currentResponse
                _isLoading.value = false
                // 延迟清空 currentResponse，确保 TTS 能获取到文本
                _currentResponse.value = ""
            },
            onError = { err ->
                _currentResponse.value = "[出错了] $err"
                _isLoading.value = false
            }
        )
    }

    fun stopGeneration() {
        engine.stop()
        _isLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }

    data class ChatMessage(val content: String, val isUser: Boolean)
}
