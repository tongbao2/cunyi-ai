package com.cunyi.ai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * 村医AI Application
 * 负责全局初始化
 */
class CunYiAIApplication : Application() {

    companion object {
        const val CHANNEL_SOS = "sos_channel"
        const val CHANNEL_ALERT = "alert_channel"
        lateinit var instance: CunYiAIApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // SOS 紧急通道
            val sosChannel = NotificationChannel(
                CHANNEL_SOS,
                "紧急求救",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "紧急求救通知"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            // 健康警报通道
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT,
                "健康警报",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "血压、血糖异常警报"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(sosChannel, alertChannel))
        }
    }
}
