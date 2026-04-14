package com.cunyi.ai.manager

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * 语音输入工具 - 使用系统语音识别 Activity
 * 比 SpeechRecognizer 更稳定可靠
 */

/**
 * 在 Composable 中注册语音识别 launcher
 * 返回一个 launcher，点击麦克风时调用 launcher.launch(...)
 */
@Composable
fun rememberVoiceInputLauncher(
    onResult: (String) -> Unit
): ActivityResultLauncher<Intent> {
    val context = LocalContext.current
    return androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = results?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                onResult(text)
            }
        }
    }
}

/**
 * 创建语音识别 Intent
 */
fun createVoiceIntent(): Intent {
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出您的问题")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
}
