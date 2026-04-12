package com.cunyi.ai.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.cunyi.ai.data.EmergencyContact
import com.cunyi.ai.data.HealthRecord
import com.cunyi.ai.data.RecordType
import com.cunyi.ai.data.SOSMessage
import org.json.JSONArray
import org.json.JSONObject

/**
 * SOS 紧急求救管理器
 */
class SOSManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val healthRecordManager = HealthRecordManager(context)

    companion object {
        private const val PREFS_NAME = "sos_contacts"
        private const val KEY_CONTACTS = "emergency_contacts"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_LOCATION = "user_location"
    }

    /**
     * 添加紧急联系人
     */
    fun addContact(contact: EmergencyContact) {
        val contacts = getContacts().toMutableList()
        if (contacts.size >= 5) {
            contacts.removeAt(0) // 最多5个联系人
        }
        contacts.add(contact)
        saveContacts(contacts)
    }

    /**
     * 获取所有紧急联系人
     */
    fun getContacts(): List<EmergencyContact> {
        val jsonString = prefs.getString(KEY_CONTACTS, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val contacts = mutableListOf<EmergencyContact>()
        
        for (i in 0 until jsonArray.length()) {
            contacts.add(jsonToContact(jsonArray.getJSONObject(i)))
        }
        
        return contacts.sortedByDescending { it.isPrimary }
    }

    /**
     * 删除联系人
     */
    fun deleteContact(contactId: Long) {
        val contacts = getContacts().filter { it.id != contactId }
        saveContacts(contacts)
    }

    /**
     * 设置为主要联系人
     */
    fun setPrimaryContact(contactId: Long) {
        val contacts = getContacts().map { contact ->
            contact.copy(isPrimary = contact.id == contactId)
        }
        saveContacts(contacts)
    }

    /**
     * 发送SOS短信
     * @param symptoms 症状描述列表
     * @param callback 回调
     */
    fun sendSOS(
        symptoms: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // 检查权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            onError("需要短信权限才能发送紧急求救")
            return
        }

        val contacts = getContacts()
        if (contacts.isEmpty()) {
            onError("请先添加紧急联系人")
            return
        }

        // 获取最近的健康记录
        val recentRecords = healthRecordManager.getRecords().take(5)
        val recentRecordStrings = formatRecentRecords(recentRecords)

        // 构建SOS消息
        val sosMessage = SOSMessage(
            patientName = getUserName(),
            timestamp = System.currentTimeMillis(),
            symptoms = symptoms,
            location = getUserLocation(),
            recentRecords = recentRecordStrings
        )

        val smsContent = sosMessage.toSMSContent()

        // 发送短信
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            
            contacts.forEach { contact ->
                val parts = smsManager.divideMessage(smsContent)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(
                        contact.phone,
                        null,
                        parts,
                        null,
                        null
                    )
                } else {
                    smsManager.sendTextMessage(
                        contact.phone,
                        null,
                        smsContent,
                        null,
                        null
                    )
                }
            }
            
            onSuccess()
        } catch (e: Exception) {
            onError("发送失败: ${e.message}")
        }
    }

    /**
     * 格式化最近健康记录
     */
    private fun formatRecentRecords(records: List<HealthRecord>): List<String> {
        return records.mapNotNull { record ->
            when (record.type) {
                RecordType.BLOOD_PRESSURE -> {
                    "血压 ${record.value.toInt()}/${record.secondaryValue?.toInt() ?: 0}mmHg (${record.getFormattedTime()})"
                }
                RecordType.BLOOD_SUGAR -> {
                    "血糖 ${record.value}mmol/L (${record.getFormattedTime()})"
                }
                RecordType.HEART_RATE -> {
                    "心率 ${record.value.toInt()}次/分 (${record.getFormattedTime()})"
                }
                else -> null
            }
        }
    }

    /**
     * 设置用户名
     */
    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    /**
     * 获取用户名
     */
    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "") ?: ""
    }

    /**
     * 设置位置
     */
    fun setUserLocation(location: String) {
        prefs.edit().putString(KEY_USER_LOCATION, location).apply()
    }

    /**
     * 获取位置
     */
    fun getUserLocation(): String {
        return prefs.getString(KEY_USER_LOCATION, "") ?: ""
    }

    private fun saveContacts(contacts: List<EmergencyContact>) {
        val jsonArray = JSONArray()
        contacts.forEach { contact ->
            jsonArray.put(contactToJson(contact))
        }
        prefs.edit().putString(KEY_CONTACTS, jsonArray.toString()).apply()
    }

    private fun contactToJson(contact: EmergencyContact): JSONObject {
        return JSONObject().apply {
            put("id", contact.id)
            put("name", contact.name)
            put("phone", contact.phone)
            put("relationship", contact.relationship)
            put("isPrimary", contact.isPrimary)
        }
    }

    private fun jsonToContact(json: JSONObject): EmergencyContact {
        return EmergencyContact(
            id = json.getLong("id"),
            name = json.getString("name"),
            phone = json.getString("phone"),
            relationship = json.optString("relationship", ""),
            isPrimary = json.optBoolean("isPrimary", false)
        )
    }
}
