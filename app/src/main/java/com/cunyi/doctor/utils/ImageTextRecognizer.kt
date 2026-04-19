package com.cunyi.doctor.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 图像文字识别工具
 * 使用 ML Kit 识别图片中的文字
 */
object ImageTextRecognizer {
    
    // 中文识别器（支持中英混排）
    private val recognizer: TextRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    
    /**
     * 从文件路径识别文字
     * @param imagePath 图片文件路径
     * @return 识别到的文字，如果识别失败返回 null
     */
    suspend fun recognizeFromPath(imagePath: String): String? {
        return try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null) {
                recognizeFromBitmap(bitmap)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 从 URI 识别文字
     * @param context Android 上下文
     * @param uri 图片 URI
     * @return 识别到的文字，如果识别失败返回 null
     */
    suspend fun recognizeFromUri(context: Context, uri: Uri): String? {
        return try {
            val inputImage = InputImage.fromFilePath(context, uri)
            recognize(inputImage)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 从 Bitmap 识别文字
     * @param bitmap 图片 Bitmap
     * @return 识别到的文字，如果识别失败返回 null
     */
    suspend fun recognizeFromBitmap(bitmap: Bitmap): String? {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognize(inputImage)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 通用识别方法
     */
    private suspend fun recognize(inputImage: InputImage): String? {
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val result = visionText.text
                    if (result.isNotBlank()) {
                        continuation.resume(result)
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    continuation.resume(null)
                }
        }
    }
    
    /**
     * 释放资源
     */
    fun close() {
        recognizer.close()
    }
}
