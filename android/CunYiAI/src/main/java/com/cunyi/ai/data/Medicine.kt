package com.cunyi.ai.data

/**
 * 药品数据类
 */
data class Medicine(
    val id: Int,
    val name: String,              // 药品名称
    val genericName: String,       // 通用名
    val usage: String,             // 用法用量
    val timing: String,            // 服用时间（饭前/饭后/随餐）
    val precautions: String,        // 注意事项
    val sideEffects: String,       // 副作用
    val contraindications: String, // 禁忌
    val category: MedicineCategory // 药品分类
)

enum class MedicineCategory {
    CARDIOVASCULAR,     // 心血管
    DIABETES,          // 糖尿病
    ANTIBIOTIC,        // 抗生素
    PAIN_RELIEF,       // 止痛
    ANTIHYPERTENSION,  // 降压
    DIGESTIVE,         // 消化
    RESPIRATORY,       // 呼吸
    ANTIPLATELET       // 抗血小板
}

/**
 * 药物相互作用
 */
data class DrugInteraction(
    val drug1: String,        // 药品1名称
    val drug2: String,        // 药品2名称
    val riskLevel: RiskLevel, // 风险等级
    val description: String  // 相互作用描述
)

enum class RiskLevel {
    HIGH,   // 高风险 - 禁止合用
    MEDIUM, // 中风险 - 谨慎合用
    LOW     // 低风险 - 注意监测
}
