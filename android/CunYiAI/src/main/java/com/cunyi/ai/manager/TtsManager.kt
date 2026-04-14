package com.cunyi.ai.manager

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

/**
 * TTS 语音播报管理器
 */
class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
            isReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (isReady) {
                tts.setSpeechRate(0.9f)  // 稍慢一点，老年人更容易听清
                tts.setPitch(1.0f)
            }
        }
    }

    /**
     * 播报文本
     */
    fun speak(text: String) {
        if (!isReady) return
        // 去掉 emoji 和特殊符号，TTS 读不了
        val cleanText = text
            .replace(Regex("[\\p{So}\\p{Cn}]"), "") // emoji
            .replace(Regex("[•→📍💉🏥🩺💊🏃‍💤📉🚨⚠️📢✅❌🌡️🍎😴]"), "")
            .replace("•", "")
            .replace("→", "")
            .trim()

        if (cleanText.isNotBlank()) {
            tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
        }
    }

    /**
     * 停止播报
     */
    fun stop() {
        tts.stop()
    }

    /**
     * 是否正在播报
     */
    fun isSpeaking(): Boolean = tts.isSpeaking

    /**
     * 释放资源
     */
    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
