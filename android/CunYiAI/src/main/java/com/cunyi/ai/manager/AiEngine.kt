package com.cunyi.ai.manager

/**
 * AI聊天引擎
 * 支持闲聊和健康咨询，已移除规则引擎
 * 当前使用内置智能回复，后续可接入云端LLM API
 */
object AiEngine {

    /**
     * 生成AI回复
     */
    fun generateResponse(userMessage: String): String {
        val msg = userMessage.trim()
        if (msg.isBlank()) return "请说点什么吧～"

        // 问候
        if (msg.matches(Regex(".*(你好|您好|hi|hello|嗨|hey).*", RegexOption.IGNORE_CASE))) {
            return "你好！我是村医AI助手🏥 有什么可以帮你的吗？可以跟我聊天，也可以咨询健康问题哦～"
        }

        // 自我介绍
        if (msg.matches(Regex(".*(你是谁|什么ai|介绍.*自己|你叫什么).*", RegexOption.IGNORE_CASE))) {
            return "我是村医AI🤖，你的健康助手！可以陪你聊天，也能回答健康问题。虽然我不是专业医生，但可以给你一些建议和参考。有紧急情况记得拨打120哦！"
        }

        // 感谢
        if (msg.matches(Regex(".*(谢谢|感谢|多谢|thanks).*", RegexOption.IGNORE_CASE))) {
            return randomPick("不客气！随时找我聊～😊", "能帮到你就好！💪", "不用谢～有需要再找我哦！")
        }

        // 心情/情绪
        if (msg.matches(Regex(".*(不开心|难过|伤心|郁闷|烦|焦虑|害怕|担心|孤独|寂寞).*", RegexOption.IGNORE_CASE))) {
            return "听起来你心情不太好😔 愿意跟我说说吗？\n\n有时候跟人聊聊会舒服很多。如果持续觉得不好，建议跟家人或朋友说说，也可以寻求专业帮助。我一直在的！"
        }

        // 天气
        if (msg.matches(Regex(".*(天气|下雨|晴天|冷|热).*", RegexOption.IGNORE_CASE))) {
            return "我暂时还不能查天气呢😅 你可以看看手机上的天气应用。提醒一下，天气变化时注意增减衣物，尤其是老年朋友要特别注意保暖！"
        }

        // 吃饭/饮食
        if (msg.matches(Regex(".*(吃什么|好吃|饿了|做饭|饮食|食谱).*", RegexOption.IGNORE_CASE))) {
            return randomPick(
                "今天想吃点什么呢？🍎 建议荤素搭配，多吃蔬菜水果，少油少盐更健康！",
                "好好吃饭很重要哦！建议多吃粗粮、蔬菜，适量蛋白质，少吃油炸食品～",
                "不知道吃什么的话，试试时令蔬菜？清淡饮食对身体健康很有好处😊"
            )
        }

        // 睡眠
        if (msg.matches(Regex(".*(睡不着|失眠|睡眠|熬夜|做梦|早醒).*", RegexOption.IGNORE_CASE))) {
            return "😴 睡眠小建议：\n\n1. 固定作息时间，每天同一时间睡觉\n2. 睡前少看手机，可以听听轻音乐\n3. 卧室保持安静、黑暗\n4. 睡前不要喝茶和咖啡\n5. 适当运动有助于睡眠\n\n如果长期失眠，建议去看医生哦～"
        }

        // 运动
        if (msg.matches(Regex(".*(运动|锻炼|跑步|散步|健身|太极).*", RegexOption.IGNORE_CASE))) {
            return "🏃 运动建议：\n\n1. 每天散步30分钟就是很好的运动\n2. 太极拳很适合中老年朋友\n3. 运动要循序渐进，别勉强\n4. 饭后1小时再运动\n5. 运动时注意安全，量力而行\n\n动起来就比坐着强！加油💪"
        }

        // 血压
        if (msg.matches(Regex(".*(血压|高压|低压|收缩压|舒张压).*", RegexOption.IGNORE_CASE))) {
            return "💉 血压参考范围：\n\n• 正常：<120/80 mmHg\n• 偏高：120-139/80-89\n• 高血压：≥140/90\n\n管理建议：低盐饮食、规律运动、按时服药、定期监测。血压波动大要及时就医！"
        }

        // 血糖
        if (msg.matches(Regex(".*(血糖|糖尿病|糖化).*", RegexOption.IGNORE_CASE))) {
            return "💉 血糖参考范围：\n\n• 空腹：3.9-6.1 mmol/L\n• 餐后2h：<7.8\n• 糖尿病：空腹≥7.0 或 餐后≥11.1\n\n控糖建议：少吃精米白面，多吃粗粮蔬菜，适量运动，定期检测。"
        }

        // 头痛/头晕
        if (msg.matches(Regex(".*(头痛|头疼|头晕|头昏).*", RegexOption.IGNORE_CASE))) {
            return "🩺 头痛/头晕要注意：\n\n常见原因：血压波动、睡眠不足、颈椎问题、感冒等。\n\n建议：\n1. 先量一下血压\n2. 注意休息\n3. 如持续超过3天，或伴有呕吐、视物模糊，请就医\n4. 突发剧烈头痛请立即拨打120！"
        }

        // 胸闷/胸痛
        if (msg.matches(Regex(".*(胸闷|胸痛|胸口|心慌|心悸).*", RegexOption.IGNORE_CASE))) {
            return "🚨 胸闷/胸痛需要重视！\n\n⚠️ 如果出现以下情况请立即拨打120：\n• 胸痛持续>10分钟\n• 伴出冷汗、恶心\n• 疼痛向左肩/下颌放射\n\n平时注意：定期检查心脏、控制血压血糖血脂、避免情绪激动。"
        }

        // 咳嗽
        if (msg.matches(Regex(".*(咳嗽|咳痰|嗓子|喉咙).*", RegexOption.IGNORE_CASE))) {
            return "🩺 咳嗽建议：\n\n1. 多喝温水，保持呼吸道湿润\n2. 观察痰的颜色（黄绿色可能感染）\n3. 避免刺激性食物\n4. 超过2周或有血痰、发热，请就医"
        }

        // 发热
        if (msg.matches(Regex(".*(发烧|发热|体温|感冒).*", RegexOption.IGNORE_CASE))) {
            return "🌡️ 发热处理：\n\n• 37.3-38℃ 低热：多喝水、休息\n• 38.5℃以上：可吃退热药\n• 物理降温：温水擦浴\n• 持续高热>3天或伴皮疹请就医"
        }

        // 腰痛/关节
        if (msg.matches(Regex(".*(腰痛|腰疼|关节|腿疼|膝盖).*", RegexOption.IGNORE_CASE))) {
            return "🩺 腰痛/关节痛建议：\n\n1. 注意保暖，避免受凉\n2. 适度活动，不要久坐久站\n3. 如伴红肿热痛，可能发炎\n4. 持续加重请就医检查"
        }

        // 用药
        if (msg.matches(Regex(".*(吃药|服药|用药|药量|副作用).*", RegexOption.IGNORE_CASE))) {
            return "💊 用药提醒：\n\n1. 严格遵医嘱服药\n2. 不要自行停药或加减量\n3. 注意药物副作用\n4. 定期复查让医生评估\n5. 不同药物间注意相互作用\n\n具体用药问题请咨询医生！"
        }

        // 就医
        if (msg.matches(Regex(".*(去医院|看医生|门诊|挂号|检查).*", RegexOption.IGNORE_CASE))) {
            return "🏥 就医建议：\n\n• 常见病可先去社区卫生中心\n• 复杂病情去县级医院\n• 急症直接拨打120！\n\n就医时带好：\n1. 身份证和医保卡\n2. 以前的检查报告\n3. 正在吃的药"
        }

        // 闲聊 - 笑话
        if (msg.matches(Regex(".*(笑话|搞笑|逗我|开心|有趣).*", RegexOption.IGNORE_CASE))) {
            return randomPick(
                "来一个！😄\n\n医生：你这是缺乏运动导致的。\n患者：那我多走路？\n医生：不，你得多来医院走动走动，我房租还没交呢～",
                "哈哈😄\n\n患者：医生，我睡觉总流口水怎么办？\n医生：你睡觉时把嘴闭上不就行了！\n患者：…",
                "来一个冷的❄️\n\n为什么程序员总是分不清万圣节和圣诞节？\n因为 Oct 31 = Dec 25 🤓"
            )
        }

        // 闲聊 - 讲故事
        if (msg.matches(Regex(".*(讲故事|故事|闲聊|聊天|无聊).*", RegexOption.IGNORE_CASE))) {
            return randomPick(
                "讲个小故事吧📖\n\n有个老人每天早起散步，邻居问他为什么这么勤快。他说：'因为我想多看看这个世界呀！' \n\n其实健康就是这样，每天坚持一点点，身体就会回报你😊",
                "来聊聊天吧！😊\n\n你今天过得怎么样？有什么想说的都可以跟我聊聊～我虽然是个AI，但我会认真听的！",
                "无聊了呀～那我给你出个谜语吧🤔\n\n什么水不能喝？\n.\n.\n.\n答案是：薪水！😂"
            )
        }

        // 默认回复 - 友好闲聊
        return randomPick(
            "嗯嗯，我听到了～${pickFollowUp()}",
            "有意思！${pickFollowUp()}",
            "好的呀～${pickFollowUp()}",
            "你说得对！${pickFollowUp()}",
            "我能理解～如果有什么健康问题也可以问我哦！🏥"
        )
    }

    fun isModelReady(): Boolean = true

    fun getModelStatus(): String = "AI已就绪"

    private fun randomPick(vararg options: String): String = options.random()

    private fun pickFollowUp(): String = listOf(
        "还有什么想聊的吗？",
        "有健康问题也可以问我哦🏥",
        "你也可以试试语音跟我说话🎤",
        "想了解什么都可以问我～",
        "有什么需要帮忙的吗？"
    ).random()
}
