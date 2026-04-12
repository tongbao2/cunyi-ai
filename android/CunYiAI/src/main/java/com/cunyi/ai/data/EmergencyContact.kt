package com.cunyi.ai.data

/**
 * 紧急联系人
 */
data class EmergencyContact(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val phone: String,
    val relationship: String = "", // 关系：子女/配偶/邻居
    val isPrimary: Boolean = false // 是否为主要联系人
)
