package com.cunyi.ai.data

import java.text.SimpleDateFormat
import java.util.*

/**
 * 健康记录数据类
 */
data class HealthRecord(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: RecordType,
    val value: Float,
    val secondaryValue: Float? = null, // 用于舒张压等
    val note: String = ""
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

enum class RecordType {
    BLOOD_PRESSURE,  // 血压
    BLOOD_SUGAR,     // 血糖
    HEART_RATE,      // 心率
    BODY_TEMPERATURE // 体温
}

/**
 * 健康警报
 */
data class HealthAlert(
    val level: AlertLevel,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val record: HealthRecord? = null
)

enum class AlertLevel {
    RED,    // 红色警报 - 立即就医
    ORANGE, // 橙色警报 - 尽快就医
    YELLOW, // 黄色警报 - 关注
    GREEN   // 绿色 - 正常
}
