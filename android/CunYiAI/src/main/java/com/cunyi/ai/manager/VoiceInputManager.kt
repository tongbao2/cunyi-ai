package com.cunyi.ai.manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "VoiceInputManager"

/**
 * 语音输入管理器
 * 使用 Android SpeechRecognizer 实现按住说话
 */
class VoiceInputManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    sealed class VoiceState {
        object Idle : VoiceState()
        object Ready : VoiceState()
        object Listening : VoiceState()
        data class Result(val text: String) : VoiceState()
        data class Error(val message: String) : VoiceState()
    }

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState

    private var onResultCallback: ((String) -> Unit)? = null

    fun initialize() {
        Log.d(TAG, "initialize: checking availability")
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "initialize: SpeechRecognizer not available")
            _voiceState.value = VoiceState.Error("当前设备不支持语音识别")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }
        Log.d(TAG, "initialize: SpeechRecognizer created successfully")
        _voiceState.value = VoiceState.Ready
    }

    /**
     * 开始录音（按住说话）
     */
    fun startListening(onResult: (String) -> Unit) {
        Log.d(TAG, "startListening: called")
        onResultCallback = onResult
        if (speechRecognizer == null) {
            Log.d(TAG, "startListening: initializing SpeechRecognizer")
            initialize()
        }

        // 确保处于正确状态
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "startListening: cancel failed", e)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            Log.d(TAG, "startListening: calling startListening")
            speechRecognizer?.startListening(intent)
            _voiceState.value = VoiceState.Listening
        } catch (e: Exception) {
            _voiceState.value = VoiceState.Error("启动语音识别失败: ${e.message}")
        }
    }

    /**
     * 停止录音
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // ignore
        }
        _voiceState.value = VoiceState.Ready
    }

    /**
     * 销毁
     */
    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // ignore
        }
        speechRecognizer = null
        _voiceState.value = VoiceState.Idle
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "onReadyForSpeech: ready to receive speech")
            _voiceState.value = VoiceState.Listening
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech: speech detected")
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech: speech ended")
        }

        override fun onError(error: Int) {
            Log.e(TAG, "onError: error code = $error")
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "音频录制错误"
                SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足，请授予麦克风权限"
                SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                SpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音，请重试"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别服务忙碌，请稍后"
                SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "未检测到语音输入"
                else -> "识别错误: $error"
            }
            Log.e(TAG, "onError: $message")
            _voiceState.value = VoiceState.Error(message)
        }

        override fun onResults(results: Bundle?) {
            Log.d(TAG, "onResults: received results")
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            Log.d(TAG, "onResults: recognized text = '$text'")
            if (text.isNotBlank()) {
                _voiceState.value = VoiceState.Result(text)
                onResultCallback?.invoke(text)
            } else {
                _voiceState.value = VoiceState.Error("未识别到语音内容，请重试")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // 实时转写（部分结果）
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            // 可以在这里更新 UI 显示中间结果
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
