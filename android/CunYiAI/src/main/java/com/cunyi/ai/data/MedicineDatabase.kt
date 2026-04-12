package com.cunyi.ai.data

/**
 * 农村常用药品数据库
 * 包含15种常见药品及药物相互作用
 */
object MedicineDatabase {

    /**
     * 获取所有药品
     */
    fun getAllMedicines(): List<Medicine> = listOf(
        // 心血管类
        Medicine(
            id = 1,
            name = "硝苯地平",
            genericName = "Nifedipine",
            usage = "每次5-10mg，每日3次",
            timing = "饭后服用",
            precautions = "可能引起头痛、面部潮红、脚踝水肿",
            sideEffects = "头痛、头晕、低血压",
            contraindications = "严重低血压、心源性休克禁用",
            category = MedicineCategory.CARDIOVASCULAR
        ),
        Medicine(
            id = 2,
            name = "氨氯地平",
            genericName = "Amlodipine",
            usage = "每次5mg，每日1次",
            timing = "早晨服用",
            precautions = "用药期间避免葡萄柚汁",
            sideEffects = "外周水肿、头痛、乏力",
            contraindications = "对二氢吡啶类钙通道阻滞剂过敏者禁用",
            category = MedicineCategory.ANTIHYPERTENSION
        ),
        Medicine(
            id = 3,
            name = "卡托普利",
            genericName = "Captopril",
            usage = "每次12.5-25mg，每日2-3次",
            timing = "饭前1小时服用",
            precautions = "可能出现干咳，味觉改变",
            sideEffects = "干咳、血管神经性水肿、高血钾",
            contraindications = "双侧肾动脉狭窄、妊娠禁用",
            category = MedicineCategory.ANTIHYPERTENSION
        ),
        Medicine(
            id = 4,
            name = "美托洛尔",
            genericName = "Metoprolol",
            usage = "每次25-50mg，每日2次",
            timing = "饭后服用",
            precautions = "突然停药可能导致心绞痛加重",
            sideEffects = "心动过缓、乏力、支气管痉挛",
            contraindications = "严重心动过缓、II-III度房室传导阻滞禁用",
            category = MedicineCategory.CARDIOVASCULAR
        ),
        Medicine(
            id = 5,
            name = "阿司匹林",
            genericName = "Aspirin",
            usage = "每次75-100mg，每日1次",
            timing = "晚饭后服用",
            precautions = "肠溶片不可掰开服用",
            sideEffects = "胃肠道不适、出血风险",
            contraindications = "对阿司匹林过敏、哮喘、胃溃疡禁用",
            category = MedicineCategory.ANTIPLATELET
        ),

        // 糖尿病类
        Medicine(
            id = 6,
            name = "二甲双胍",
            genericName = "Metformin",
            usage = "每次0.5g，每日2-3次",
            timing = "饭后服用",
            precautions = "用药期间避免饮酒",
            sideEffects = "恶心、腹泻、维生素B12缺乏",
            contraindications = "严重肝肾功能不全、酗酒者禁用",
            category = MedicineCategory.DIABETES
        ),
        Medicine(
            id = 7,
            name = "格列齐特",
            genericName = "Gliclazide",
            usage = "每次40-80mg，每日1-2次",
            timing = "饭前30分钟服用",
            precautions = "可能导致低血糖",
            sideEffects = "低血糖、胃肠道不适",
            contraindications = "I型糖尿病、严重肝肾功能不全禁用",
            category = MedicineCategory.DIABETES
        ),

        // 抗生素类
        Medicine(
            id = 8,
            name = "阿莫西林",
            genericName = "Amoxicillin",
            usage = "每次0.5g，每日3-4次",
            timing = "饭后服用",
            precautions = "青霉素过敏者禁用",
            sideEffects = "腹泻、皮疹、恶心",
            contraindications = "对青霉素过敏者禁用",
            category = MedicineCategory.ANTIBIOTIC
        ),
        Medicine(
            id = 9,
            name = "头孢氨苄",
            genericName = "Cephalexin",
            usage = "每次250-500mg，每6小时1次",
            timing = "空腹或饭后均可",
            precautions = "头孢菌素过敏者慎用",
            sideEffects = "胃肠道不适、皮疹",
            contraindications = "对头孢菌素过敏者禁用",
            category = MedicineCategory.ANTIBIOTIC
        ),

        // 止痛类
        Medicine(
            id = 10,
            name = "对乙酰氨基酚",
            genericName = "Paracetamol",
            usage = "每次0.3-0.6g，每日3-4次，每日不超过2g",
            timing = "饭后服用",
            precautions = "肝功能不全者慎用",
            sideEffects = "偶见恶心、呕吐",
            contraindications = "严重肝肾功能不全禁用",
            category = MedicineCategory.PAIN_RELIEF
        ),
        Medicine(
            id = 11,
            name = "布洛芬",
            genericName = "Ibuprofen",
            usage = "每次200-400mg，每日3-4次",
            timing = "饭后服用",
            precautions = "胃溃疡患者慎用",
            sideEffects = "胃肠道不适、头晕",
            contraindications = "活动性消化性溃疡、对布洛芬过敏者禁用",
            category = MedicineCategory.PAIN_RELIEF
        ),

        // 消化类
        Medicine(
            id = 12,
            name = "奥美拉唑",
            genericName = "Omeprazole",
            usage = "每次20mg，每日1次",
            timing = "早餐前服用",
            precautions = "长期使用需补充维生素B12",
            sideEffects = "头痛、腹泻、恶心",
            contraindications = "对质子泵抑制剂过敏者禁用",
            category = MedicineCategory.DIGESTIVE
        ),

        // 抗凝类
        Medicine(
            id = 13,
            name = "华法林",
            genericName = "Warfarin",
            usage = "根据INR调整剂量，通常2.5-5mg",
            timing = "每日固定时间服用",
            precautions = "需要定期监测凝血功能，避免维生素K摄入大幅波动",
            sideEffects = "出血风险增加",
            contraindications = "活动性出血、妊娠禁用",
            category = MedicineCategory.ANTIPLATELET
        ),

        // 感冒药类
        Medicine(
            id = 14,
            name = "复方氨酚烷胺",
            genericName = "Compound Paracetamol",
            usage = "每次1片，每日2次",
            timing = "饭后服用",
            precautions = "服用期间避免饮酒",
            sideEffects = "轻度头晕、乏力、恶心",
            contraindications = "对本品过敏、严重肝肾功能不全者禁用",
            category = MedicineCategory.RESPIRATORY
        ),

        // 外用药
        Medicine(
            id = 15,
            name = "红霉素软膏",
            genericName = "Erythromycin Ointment",
            usage = "局部外用，每日2次",
            timing = "清洁患处后涂抹",
            precautions = "仅限外用，避免接触眼睛",
            sideEffects = "偶见局部刺激感",
            contraindications = "对红霉素过敏者禁用",
            category = MedicineCategory.ANTIBIOTIC
        )
    )

    /**
     * 根据名称搜索药品
     */
    fun searchMedicine(keyword: String): List<Medicine> {
        return getAllMedicines().filter { medicine ->
            medicine.name.contains(keyword, ignoreCase = true) ||
            medicine.genericName.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * 获取药物相互作用列表
     */
    fun getDrugInteractions(): List<DrugInteraction> = listOf(
        // 高风险相互作用
        DrugInteraction(
            drug1 = "华法林",
            drug2 = "阿司匹林",
            riskLevel = RiskLevel.HIGH,
            description = "两药合用显著增加出血风险，可能导致严重出血。如需合用应密切监测INR和出血迹象。"
        ),
        DrugInteraction(
            drug1 = "硝苯地平",
            drug2 = "葡萄柚汁",
            riskLevel = RiskLevel.HIGH,
            description = "葡萄柚汁可增加硝苯地平的血药浓度，导致血压过低。请在服药期间避免饮用葡萄柚汁。"
        ),
        DrugInteraction(
            drug1 = "卡托普利",
            drug2 = "保钾利尿剂",
            riskLevel = RiskLevel.MEDIUM,
            description = "两药合用可能引起高血钾。如需合用应密切监测血钾水平。"
        ),
        // 中风险相互作用
        DrugInteraction(
            drug1 = "二甲双胍",
            drug2 = "酒精",
            riskLevel = RiskLevel.HIGH,
            description = "酒精增加二甲双胍引起乳酸酸中毒的风险。服药期间应避免饮酒。"
        ),
        DrugInteraction(
            drug1 = "阿司匹林",
            drug2 = "布洛芬",
            riskLevel = RiskLevel.MEDIUM,
            description = "布洛芬可能降低阿司匹林的心血管保护作用。如需合用应在医生指导下使用。"
        ),
        DrugInteraction(
            drug1 = "美托洛尔",
            drug2 = "氨氯地平",
            riskLevel = RiskLevel.MEDIUM,
            description = "两药合用可能导致严重心动过缓或低血压。如需合用应密切监测心率和血压。"
        ),
        DrugInteraction(
            drug1 = "格列齐特",
            drug2 = "酒精",
            riskLevel = RiskLevel.MEDIUM,
            description = "酒精可能增强格列齐特的降血糖作用，导致低血糖。服药期间应避免饮酒。"
        ),
        DrugInteraction(
            drug1 = "对乙酰氨基酚",
            drug2 = "酒精",
            riskLevel = RiskLevel.MEDIUM,
            description = "长期饮酒者服用对乙酰氨基酚可能增加肝损伤风险。"
        ),
        // 低风险相互作用
        DrugInteraction(
            drug1 = "头孢氨苄",
            drug2 = "丙磺舒",
            riskLevel = RiskLevel.LOW,
            description = "丙磺舒可能增加头孢氨苄的血药浓度和半衰期。"
        ),
        DrugInteraction(
            drug1 = "奥美拉唑",
            drug2 = "氯吡格雷",
            riskLevel = RiskLevel.MEDIUM,
            description = "奥美拉唑可能降低氯吡格雷的抗血小板效果。如需合用可考虑使用泮托拉唑。"
        )
    )

    /**
     * 检查药物相互作用
     * @param medicineNames 当前服用的药品名称列表
     * @return 相互作用列表
     */
    fun checkInteractions(medicineNames: List<String>): List<DrugInteraction> {
        val interactions = mutableListOf<DrugInteraction>()
        
        for (interaction in getDrugInteractions()) {
            val drug1Match = medicineNames.any { 
                it.contains(interaction.drug1, ignoreCase = true) ||
                interaction.drug1.contains(it, ignoreCase = true)
            }
            val drug2Match = medicineNames.any { 
                it.contains(interaction.drug2, ignoreCase = true) ||
                interaction.drug2.contains(it, ignoreCase = true)
            }
            
            if (drug1Match && drug2Match) {
                interactions.add(interaction)
            }
        }
        
        return interactions.sortedByDescending { it.riskLevel }
    }
}
