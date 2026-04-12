package com.cunyi.ai.data

/**
 * 紧急求救消息
 */
data class SOSMessage(
    val patientName: String,
    val timestamp: Long,
    val symptoms: List<String>,
    val location: String = "",
    val recentRecords: List<String> = emptyList()
) {
    fun toSMSContent(): String {
        val sb = StringBuilder()
        sb.append("【村医AI紧急提醒】")
        if (patientName.isNotEmpty()) {
            sb.append("${patientName}于")
        }
        val time = java.text.SimpleDateFormat("MM月dd日 HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
        sb.append("$time")
        if (symptoms.isNotEmpty()) {
            sb.append(" 感到${symptoms.joinToString("、")}")
        }
        if (location.isNotEmpty()) {
            sb.append("\n位置：$location")
        }
        sb.append("\n请立即联系或前往")
        return sb.toString()
    }
}
