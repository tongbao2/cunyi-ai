package com.cunyi.ai.manager

import com.cunyi.ai.data.HealthRecord
import com.cunyi.ai.data.RecordType
import com.cunyi.ai.data.AlertLevel
import com.cunyi.ai.data.HealthAlert

/**
 * 规则引擎 - 安全兜底
 * 使用硬编码的医学阈值，不依赖 AI 模型
 */
object RulesEngine {

    // ========== 血压阈值 ==========
    // 收缩压 (高压)
    private const val BP_SYSTOLIC_RED = 180f  // 红色警报
    private const val BP_SYSTOLIC_ORANGE = 160f // 橙色警报
    private const val BP_SYSTOLIC_YELLOW = 140f // 黄色警报
    private const val BP_SYSTOLIC_LOW = 90f    // 低血压警告

    // 舒张压 (低压)
    private const val BP_DIASTOLIC_RED = 120f   // 红色警报
    private const val BP_DIASTOLIC_ORANGE = 100f // 橙色警报
    private const val BP_DIASTOLIC_YELLOW = 90f  // 黄色警报
    private const val BP_DIASTOLIC_LOW = 60f    // 低血压警告

    // ========== 血糖阈值 ==========
    private const val BS_RED_HIGH = 16.7f      // 高血糖危险
    private const val BS_RED_LOW = 2.8f        // 低血糖危险
    private const val BS_ORANGE_HIGH = 13.9f  // 橙色警报
    private const val BS_ORANGE_LOW = 3.9f    // 橙色警报（低）
    private const val BS_YELLOW_HIGH = 10.0f  // 黄色警报
    private const val BS_YELLOW_LOW = 4.4f    // 黄色警报（低）

    // ========== 心率阈值 ==========
    private const val HR_RED_HIGH = 150f
    private const val HR_RED_LOW = 40f
    private const val HR_ORANGE_HIGH = 120f
    private const val HR_ORANGE_LOW = 50f
    private const val HR_YELLOW_HIGH = 100f
    private const val HR_YELLOW_LOW = 60f

    /**
     * 检查血压并返回警报级别
     */
    fun checkBloodPressure(systolic: Float, diastolic: Float): HealthAlert {
        val level: AlertLevel
        val title: String
        val message: String

        // 最高优先级的检查
        if (systolic > BP_SYSTOLIC_RED || diastolic > BP_DIASTOLIC_RED) {
            level = AlertLevel.RED
            title = "血压危险！立即就医！"
            message = buildString {
                append("收缩压 ${systolic.toInt()}mmHg，舒张压 ${diastolic.toInt()}mmHg\n")
                append("血压严重超标，可能有生命危险！\n")
                append("请立即拨打120或前往最近医院！")
            }
        } else if (systolic < BP_SYSTOLIC_LOW || diastolic < BP_DIASTOLIC_LOW) {
            level = AlertLevel.RED
            title = "血压过低！"
            message = "血压偏低，可能导致晕厥。请坐下休息并尽快就医。"
        } else if (systolic > BP_SYSTOLIC_ORANGE || diastolic > BP_DIASTOLIC_ORANGE) {
            level = AlertLevel.ORANGE
            title = "血压偏高！"
            message = buildString {
                append("收缩压 ${systolic.toInt()}mmHg，舒张压 ${diastolic.toInt()}mmHg\n")
                append("血压偏高，建议尽快就医调整用药。")
            }
        } else if (systolic > BP_SYSTOLIC_YELLOW || diastolic > BP_DIASTOLIC_YELLOW) {
            level = AlertLevel.YELLOW
            title = "血压轻度升高"
            message = "血压略高，建议清淡饮食、适当运动，密切关注。"
        } else {
            level = AlertLevel.GREEN
            title = "血压正常"
            message = "血压在正常范围内。请继续保持健康生活方式。"
        }

        val record = HealthRecord(
            timestamp = System.currentTimeMillis(),
            type = RecordType.BLOOD_PRESSURE,
            value = systolic,
            secondaryValue = diastolic
        )

        return HealthAlert(level, title, message, record = record)
    }

    /**
     * 检查血糖
     * @param value 血糖值 (mmol/L)
     */
    fun checkBloodSugar(value: Float, isFasting: Boolean = true): HealthAlert {
        val level: AlertLevel
        val title: String
        val message: String

        if (value > BS_RED_HIGH) {
            level = AlertLevel.RED
            title = "血糖危险！立即就医！"
            message = "血糖 ${value}mmol/L，严重超标！可能导致酮症酸中毒，请立即就医！"
        } else if (value < BS_RED_LOW) {
            level = AlertLevel.RED
            title = "血糖危险！"
            message = "血糖 ${value}mmol/L，严重偏低！可能导致低血糖昏迷，请立即补充糖分并就医！"
        } else if (value > BS_ORANGE_HIGH) {
            level = AlertLevel.ORANGE
            title = "血糖偏高"
            message = "血糖 ${value}mmol/L，偏高。请注意控制饮食，增加运动，或咨询医生调整用药。"
        } else if (value < BS_ORANGE_LOW) {
            level = AlertLevel.ORANGE
            title = "血糖偏低"
            message = "血糖 ${value}mmol/L，偏低。请适当补充糖分，避免低血糖反应。"
        } else if (value > BS_YELLOW_HIGH) {
            level = AlertLevel.YELLOW
            title = "血糖略高"
            message = "血糖 ${value}mmol/L，略高。请继续控制饮食，密切关注。"
        } else if (value < BS_YELLOW_LOW && isFasting) {
            level = AlertLevel.YELLOW
            title = "血糖略低"
            message = "血糖 ${value}mmol/L，略低。请注意营养均衡。"
        } else {
            level = AlertLevel.GREEN
            title = "血糖正常"
            message = "血糖 ${value}mmol/L，在正常范围内。请继续保持。"
        }

        val record = HealthRecord(
            timestamp = System.currentTimeMillis(),
            type = RecordType.BLOOD_SUGAR,
            value = value
        )

        return HealthAlert(level, title, message, record = record)
    }

    /**
     * 检查心率
     * @param value 心率 (次/分钟)
     */
    fun checkHeartRate(value: Float): HealthAlert {
        val level: AlertLevel
        val title: String
        val message: String

        if (value > HR_RED_HIGH) {
            level = AlertLevel.RED
            title = "心率过快！"
            message = "心率 ${value.toInt()}次/分，严重过快！可能存在心律失常，请立即就医！"
        } else if (value < HR_RED_LOW) {
            level = AlertLevel.RED
            title = "心率过慢！"
            message = "心率 ${value.toInt()}次/分，严重过慢！可能存在心脏传导问题，请立即就医！"
        } else if (value > HR_ORANGE_HIGH) {
            level = AlertLevel.ORANGE
            title = "心率偏快"
            message = "心率 ${value.toInt()}次/分，偏快。建议休息片刻后复测，如有不适请就医。"
        } else if (value < HR_ORANGE_LOW) {
            level = AlertLevel.ORANGE
            title = "心率偏慢"
            message = "心率 ${value.toInt()}次/分，偏慢。建议就医检查心脏功能。"
        } else if (value > HR_YELLOW_HIGH) {
            level = AlertLevel.YELLOW
            title = "心率略快"
            message = "心率 ${value.toInt()}次/分，略快。请注意休息，避免剧烈运动。"
        } else if (value < HR_YELLOW_LOW) {
            level = AlertLevel.YELLOW
            title = "心率略慢"
            message = "心率 ${value.toInt()}次/分，略慢。如无头晕乏力等症状，可继续观察。"
        } else {
            level = AlertLevel.GREEN
            title = "心率正常"
            message = "心率 ${value.toInt()}次/分，在正常范围内。请继续保持。"
        }

        val record = HealthRecord(
            timestamp = System.currentTimeMillis(),
            type = RecordType.HEART_RATE,
            value = value
        )

        return HealthAlert(level, title, message, record = record)
    }

    /**
     * 检查连续趋势
     */
    fun checkTrend(records: List<HealthRecord>): HealthAlert? {
        if (records.size < 3) return null

        // 获取最近的血压记录
        val bpRecords = records
            .filter { it.type == RecordType.BLOOD_PRESSURE }
            .take(5)

        if (bpRecords.size >= 3) {
            // 检查是否连续上升
            val systolicValues = bpRecords.map { it.value }
            var risingCount = 0
            for (i in 1 until systolicValues.size) {
                if (systolicValues[i] > systolicValues[i - 1] + 5) {
                    risingCount++
                }
            }

            if (risingCount >= 2) {
                return HealthAlert(
                    level = AlertLevel.YELLOW,
                    title = "血压上升趋势",
                    message = "检测到血压连续上升趋势，建议密切关注，必要时就医。"
                )
            }
        }

        return null
    }
}
