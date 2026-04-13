package com.cunyi.ai.manager

import com.cunyi.ai.data.AlertLevel

/**
 * AI问诊引擎
 * 当前使用规则引擎 + 关键词匹配提供健康建议
 * 后续可升级为 Gemma 4 LLM 推理
 */
object AiEngine {

    // 症状关键词映射
    private val symptomKeywords = mapOf(
        listOf("头痛", "头疼", "头昏", "头晕") to SymptomCategory.HEADACHE,
        listOf("胸闷", "胸痛", "胸口") to SymptomCategory.CHEST_PAIN,
        listOf("咳嗽", "咳痰", "咳血") to SymptomCategory.COUGH,
        listOf("发烧", "发热", "体温") to SymptomCategory.FEVER,
        listOf("恶心", "呕吐", "反胃") to SymptomCategory.DIGESTIVE,
        listOf("腹泻", "拉肚子", "大便稀") to SymptomCategory.DIARRHEA,
        listOf("腰痛", "腰疼", "腰酸") to SymptomCategory.BACK_PAIN,
        listOf("关节", "关节痛", "关节疼") to SymptomCategory.JOINT_PAIN,
        listOf("皮疹", "皮肤", "瘙痒") to SymptomCategory.SKIN,
        listOf("失眠", "睡不着", "睡眠") to SymptomCategory.SLEEP,
        listOf("血压", "高压", "低压") to SymptomCategory.BLOOD_PRESSURE,
        listOf("血糖", "血糖高", "血糖低") to SymptomCategory.BLOOD_SUGAR,
        listOf("心慌", "心跳", "心率") to SymptomCategory.HEART_RATE,
    )

    private enum class SymptomCategory {
        HEADACHE, CHEST_PAIN, COUGH, FEVER, DIGESTIVE, DIARRHEA,
        BACK_PAIN, JOINT_PAIN, SKIN, SLEEP, BLOOD_PRESSURE, BLOOD_SUGAR, HEART_RATE
    }

    /**
     * 处理用户输入，返回AI回复
     */
    fun generateResponse(userMessage: String): String {
        val msg = userMessage.lowercase().trim()

        // 1. 先尝试解析健康数据查询
        val healthResponse = parseHealthQuery(msg)
        if (healthResponse != null) return healthResponse

        // 2. 尝试匹配症状
        val matchedCategory = matchSymptom(msg)
        if (matchedCategory != null) {
            return generateSymptomAdvice(matchedCategory, msg)
        }

        // 3. 通用健康问答
        return generateGeneralResponse(msg)
    }

    /**
     * 解析健康数据查询
     */
    private fun parseHealthQuery(msg: String): String? {
        // 血压查询：高压xxx低压xxx 或 血压xxx/xxx
        val bpRegex = Regex("(?:血压|高压|收缩压)[^\\d]*(\\d{2,3})(?:/|到|至)\\s*(\\d{2,3})")
        val bpMatch = bpRegex.find(msg)
        if (bpMatch != null) {
            val systolic = bpMatch.groupValues[1].toFloatOrNull() ?: return null
            val diastolic = bpMatch.groupValues[2].toFloatOrNull() ?: return null
            val alert = RulesEngine.checkBloodPressure(systolic, diastolic)
            return buildAlertResponse(alert)
        }

        // 血糖查询
        val bsRegex = Regex("(?:血糖)[^\\d]*(\\d{1,2}\\.?\\d?)")
        val bsMatch = bsRegex.find(msg)
        if (bsMatch != null) {
            val value = bsMatch.groupValues[1].toFloatOrNull() ?: return null
            val isFasting = msg.contains("空腹") || msg.contains("饭前")
            val alert = RulesEngine.checkBloodSugar(value, isFasting)
            return buildAlertResponse(alert)
        }

        // 心率查询
        val hrRegex = Regex("(?:心率|心跳|脉搏)[^\\d]*(\\d{2,3})")
        val hrMatch = hrRegex.find(msg)
        if (hrMatch != null) {
            val value = hrMatch.groupValues[1].toFloatOrNull() ?: return null
            val alert = RulesEngine.checkHeartRate(value)
            return buildAlertResponse(alert)
        }

        return null
    }

    /**
     * 匹配症状关键词
     */
    private fun matchSymptom(msg: String): SymptomCategory? {
        for ((keywords, category) in symptomKeywords) {
            if (keywords.any { it.lowercase() in msg }) {
                return category
            }
        }
        return null
    }

    /**
     * 生成症状建议
     */
    private fun generateSymptomAdvice(category: SymptomCategory, msg: String): String {
        return when (category) {
            SymptomCategory.HEADACHE -> buildString {
                appendLine("🩺 关于头痛的分析：")
                appendLine()
                append("根据您描述的情况，头痛可能由以下原因引起：")
                append("① 血压升高；② 睡眠不足或疲劳；")
                append("③ 眼部疲劳；④ 情绪紧张；⑤ 感冒发热。")
                appendLine()
                appendLine("建议措施：")
                appendLine("1. 测量血压，排除高血压")
                appendLine("2. 保证充足睡眠")
                appendLine("3. 适当休息，缓解眼部和精神疲劳")
                appendLine("4. 如持续超过3天或伴有呕吐、视物模糊，请立即就医")
            }

            SymptomCategory.CHEST_PAIN -> buildString {
                appendLine("🩺 胸闷/胸痛需要高度重视！")
                appendLine()
                append("引起胸闷胸痛的原因较多：")
                append("① 心脏问题（心绞痛、心肌缺血）；")
                append("② 肺部问题（肺炎、气胸）；")
                append("③ 消化系统（胃食管反流）；")
                append("④ 肌肉骨骼问题。")
                appendLine()
                appendLine("⚠️ 如出现以下情况，请立即拨打120：")
                appendLine("• 胸痛持续超过10分钟")
                appendLine("• 伴有出冷汗、恶心")
                appendLine("• 疼痛向左肩或下颌放射")
                appendLine("• 既往有心脏病史")
                appendLine()
                append("建议：尽快到就近医院心内科就诊，做心电图检查。")
            }

            SymptomCategory.COUGH -> buildString {
                appendLine("🩺 关于咳嗽的分析：")
                appendLine()
                append("咳嗽的常见原因：")
                append("① 感冒、流感等呼吸道感染；")
                append("② 过敏性咳嗽；")
                append("③ 慢性支气管炎；")
                append("④ 肺部感染或结核。")
                appendLine()
                appendLine("建议措施：")
                appendLine("1. 多喝温水，保持呼吸道湿润")
                appendLine("2. 如有痰，观察痰的颜色（黄绿色提示细菌感染）")
                appendLine("3. 避免刺激性食物和烟雾")
                appendLine("4. 如咳嗽超过2周或伴有血痰、发热，请就医")
            }

            SymptomCategory.FEVER -> buildString {
                appendLine("🩺 关于发热的处理建议：")
                appendLine()
                appendLine("发热的分度（腋下温度）：")
                appendLine("• 低热：37.3-38℃")
                appendLine("• 中等热：38.1-39℃")
                appendLine("• 高热：39.1-41℃")
                appendLine("• 超高热：>41℃")
                appendLine()
                appendLine("处理措施：")
                appendLine("1. 多饮水，防止脱水")
                appendLine("2. 38.5℃以上可服用退热药（布洛芬或对乙酰氨基酚）")
                appendLine("3. 物理降温（温水擦浴）")
                appendLine("4. 如持续高热>3天或出现皮疹、抽搐，请立即就医")
            }

            SymptomCategory.DIGESTIVE -> buildString {
                appendLine("🩺 关于消化不适的分析：")
                appendLine()
                append("恶心呕吐的常见原因：")
                append("① 急性胃肠炎；② 食物中毒；")
                append("③ 晕车晕船；④ 妊娠反应；")
                append("⑤ 颅内压增高（危险）。")
                appendLine()
                appendLine("建议措施：")
                appendLine("1. 清淡饮食，少量多餐")
                appendLine("2. 补充水分和电解质")
                appendLine("3. 避免油腻、辛辣食物")
                appendLine("4. 如反复呕吐、无法进食或伴有头痛，请就医")
            }

            SymptomCategory.DIARRHEA -> buildString {
                appendLine("🩺 关于腹泻的处理建议：")
                appendLine()
                appendLine("腹泻的处理原则：")
                appendLine("1. 补充水分和电解质（口服补液盐）")
                appendLine("2. 清淡饮食，避免乳制品和高脂肪食物")
                appendLine("3. 观察大便性状（血便需立即就医）")
                appendLine()
                appendLine("⚠️ 需立即就医的情况：")
                appendLine("• 大便带血或呈黑色")
                appendLine("• 腹泻超过3天无好转")
                appendLine("• 严重脱水（口干、尿少、头晕）")
                appendLine("• 发热>38.5℃")
            }

            SymptomCategory.BACK_PAIN -> buildString {
                appendLine("🩺 关于腰痛的分析：")
                appendLine()
                append("腰痛的常见原因：")
                append("① 腰肌劳损（最常见）；② 腰椎间盘突出；")
                append("③ 泌尿系统结石；④ 妇科问题（女性）。")
                appendLine()
                appendLine("建议措施：")
                appendLine("1. 避免久坐久站")
                appendLine("2. 睡硬板床")
                appendLine("3. 适度腰部锻炼")
                appendLine("4. 如伴有下肢麻木或大小便异常，请立即就医")
            }

            SymptomCategory.JOINT_PAIN -> buildString {
                appendLine("🩺 关于关节痛的分析：")
                appendLine()
                append("关节痛的常见原因：")
                append("① 骨关节炎；② 类风湿性关节炎；")
                append("③ 痛风；④ 风湿热。")
                appendLine()
                appendLine("建议措施：")
                appendLine("1. 注意关节保暖")
                appendLine("2. 避免过度使用关节")
                appendLine("3. 观察是否伴有红肿热痛（提示炎症）")
                appendLine("4. 如多关节受累或晨僵>1小时，请就医")
            }

            SymptomCategory.SKIN -> buildString {
                appendLine("🩺 关于皮肤症状的分析：")
                appendLine()
                append("常见皮肤问题：")
                append("① 湿疹、皮炎；② 荨麻疹（过敏）；")
                append("③ 带状疱疹；④ 感染性疾病。")
                appendLine()
                appendLine("建议措施：")
                appendLine("1. 保持皮肤清洁干燥")
                appendLine("2. 避免抓挠，防止感染")
                appendLine("3. 观察皮疹形态和分布")
                appendLine("4. 如皮疹扩散、发热或口腔黏膜受累，请就医")
            }

            SymptomCategory.SLEEP -> buildString {
                appendLine("🩺 关于睡眠问题的建议：")
                appendLine()
                appendLine("改善睡眠的措施：")
                appendLine("1. 固定作息时间")
                appendLine("2. 睡前避免咖啡、浓茶、手机")
                appendLine("3. 营造安静、黑暗的睡眠环境")
                appendLine("4. 适度运动，但避免睡前剧烈运动")
                appendLine()
                appendLine("如长期失眠（>3个月），建议就医寻求专业帮助")
            }

            SymptomCategory.BLOOD_PRESSURE -> buildString {
                appendLine("💉 血压健康指导：")
                appendLine()
                appendLine("正常血压范围（诊室测量）：")
                appendLine("• 正常：<120/80 mmHg")
                appendLine("• 正常高值：120-139/80-89 mmHg")
                appendLine("• 高血压：≥140/90 mmHg")
                appendLine()
                appendLine("管理建议：")
                appendLine("1. 低盐饮食（<5g/天）")
                appendLine("2. 适量运动（每周150分钟中等强度）")
                appendLine("3. 控制体重")
                appendLine("4. 按时服药，定期监测")
            }

            SymptomCategory.BLOOD_SUGAR -> buildString {
                appendLine("💉 血糖健康指导：")
                appendLine()
                appendLine("血糖正常范围（指尖血糖）：")
                appendLine("• 空腹：3.9-6.1 mmol/L")
                appendLine("• 餐后2h：<7.8 mmol/L")
                appendLine()
                appendLine("糖尿病诊断标准：")
                appendLine("• 空腹≥7.0 mmol/L")
                appendLine("• 随机或餐后≥11.1 mmol/L")
                appendLine()
                appendLine("管理建议：")
                appendLine("1. 控制碳水化合物摄入")
                appendLine("2. 适量运动")
                appendLine("3. 定期监测血糖")
                appendLine("4. 按医嘱用药")
            }

            SymptomCategory.HEART_RATE -> buildString {
                appendLine("💓 心率健康指导：")
                appendLine()
                appendLine("正常静息心率：60-100 次/分")
                appendLine("• 运动员：40-60 次/分（正常）")
                appendLine("• 心动过速：>100 次/分")
                appendLine("• 心动过缓：<60 次/分")
                appendLine()
                appendLine("建议：")
                appendLine("1. 测量前静坐5分钟")
                appendLine("2. 避免咖啡因和刺激性饮料")
                appendLine("3. 如持续心动过速或过缓，请就医")
            }
        }
    }

    /**
     * 生成通用回复
     */
    private fun generateGeneralResponse(msg: String): String {
        return when {
            // 问候
            msg.contains("你好") || msg.contains("您好") || msg.contains("hi") || msg.contains("hello") ->
                "您好！我是村医AI助手🏥\n\n请描述您的症状或健康问题，我会尽力为您提供健康建议。\n\n您也可以告诉我您的血压、血糖或心率数值，我来帮您分析。"

            // 感谢
            msg.contains("谢谢") || msg.contains("感谢") ->
                "不客气！祝您身体健康！🌿\n\n如有其他健康问题，随时可以问我。"

            // 关于AI
            msg.contains("你是谁") || msg.contains("什么ai") || msg.contains("什么模型") ->
                "我是村医AI🤖，一款面向农村老人的离线健康助手。\n\n我可以帮您：\n• 分析血压、血糖、心率\n• 给出症状健康建议\n• 提供就医指导\n\n⚠️ AI建议仅供参考，不能替代医生诊断。"

            // 就医建议
            msg.contains("去医") || msg.contains("医院") || msg.contains("门诊") ->
                "🏥 就医建议：\n\n一般常见病可先到乡镇卫生院或社区卫生服务中心就诊。\n\n如遇到以下情况，建议去县级医院：\n• 症状持续不缓解\n• 需要做检查（CT、超声等）\n• 病情较复杂\n\n急症请直接拨打120！"

            // 药物咨询
            msg.contains("吃药") || msg.contains("服药") || msg.contains("用药") ->
                "💊 用药提醒：\n\n1. 遵医嘱按时服药，不要自行停药或换药\n2. 了解药物副作用\n3. 记录用药时间和反应\n4. 定期复查，让医生评估效果\n\n如有具体用药问题，建议咨询您的医生。"

            // 饮食建议
            msg.contains("饮食") || msg.contains("吃什么") || msg.contains("食谱") ->
                "🍎 健康饮食建议：\n\n1. 少盐：每天<5克\n2. 少油：少吃肥肉和油炸食品\n3. 多蔬菜：每天500克以上\n4. 适量水果\n5. 粗细粮搭配\n\n具体饮食方案应根据您的健康状况调整。"

            // 运动建议
            msg.contains("运动") || msg.contains("锻炼") || msg.contains("跑步") ->
                "🏃 运动建议：\n\n1. 每周至少150分钟中等强度有氧运动\n2. 如快走、慢跑、骑车、游泳\n3. 运动前热身，运动后拉伸\n4. 根据身体状况调整强度\n5. 糖尿病患者运动后注意低血糖"

            else ->
                buildString {
                    appendLine("🤔 我理解您的询问。")
                    appendLine()
                    append("为了给您更好的建议，请尽量详细描述：")
                    appendLine()
                    appendLine("• 具体症状（部位、持续时间、诱因）")
                    appendLine("• 您最近的健康数据（如有）")
                    appendLine("• 是否在服用药物")
                    appendLine()
                    append("示例：\"我头痛3天了，伴有头晕\"")
                    appendLine()
                    appendLine("---")
                    appendLine("⚠️ 如有紧急情况，请拨打120！")
                }
        }
    }

    /**
     * 构建警报响应
     */
    private fun buildAlertResponse(alert: com.cunyi.ai.data.HealthAlert): String {
        val emoji = when (alert.level) {
            AlertLevel.RED -> "🚨"
            AlertLevel.ORANGE -> "⚠️"
            AlertLevel.YELLOW -> "📢"
            AlertLevel.GREEN -> "✅"
        }
        return buildString {
            appendLine("$emoji ${alert.title}")
            appendLine()
            append(alert.message)
            appendLine()
            appendLine()
            appendLine("---")
            appendLine("⚠️ 本建议仅供参考，请以医生诊断为准。如有不适，请及时就医。")
        }
    }

    /**
     * 检查模型是否可用（当前版本始终可用，因为使用规则引擎）
     */
    fun isModelReady(): Boolean = true

    /**
     * 获取模型状态描述
     */
    fun getModelStatus(): String = "AI已就绪（规则引擎模式）"
}
