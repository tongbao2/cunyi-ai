package com.cunyi.ai.manager

import android.content.Context
import android.content.SharedPreferences
import com.cunyi.ai.data.HealthRecord
import com.cunyi.ai.data.RecordType
import org.json.JSONArray
import org.json.JSONObject

/**
 * 健康记录管理器
 * 使用 SharedPreferences 存储健康数据
 */
class HealthRecordManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "health_records"
        private const val KEY_RECORDS = "records"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_LOCATION = "user_location"
        private const val MAX_RECORDS = 1000 // 最多保存1000条记录
    }

    /**
     * 保存健康记录
     */
    fun saveRecord(record: HealthRecord) {
        val records = getRecords().toMutableList()
        records.add(0, record) // 添加到列表开头
        
        // 限制记录数量
        val trimmedRecords = records.take(MAX_RECORDS)
        
        val jsonArray = JSONArray()
        trimmedRecords.forEach { r ->
            jsonArray.put(recordToJson(r))
        }
        
        prefs.edit().putString(KEY_RECORDS, jsonArray.toString()).apply()
    }

    /**
     * 获取所有健康记录
     */
    fun getRecords(): List<HealthRecord> {
        val jsonString = prefs.getString(KEY_RECORDS, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val records = mutableListOf<HealthRecord>()
        
        for (i in 0 until jsonArray.length()) {
            records.add(jsonToRecord(jsonArray.getJSONObject(i)))
        }
        
        return records
    }

    /**
     * 获取指定类型的最近记录
     */
    fun getRecentRecords(type: RecordType, limit: Int = 10): List<HealthRecord> {
        return getRecords()
            .filter { it.type == type }
            .take(limit)
    }

    /**
     * 获取今日记录
     */
    fun getTodayRecords(): List<HealthRecord> {
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        return getRecords().filter { it.timestamp >= today }
    }

    /**
     * 获取本周记录
     */
    fun getWeekRecords(): List<HealthRecord> {
        val weekAgo = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -7)
        }.timeInMillis
        
        return getRecords().filter { it.timestamp >= weekAgo }
    }

    /**
     * 删除记录
     */
    fun deleteRecord(recordId: Long) {
        val records = getRecords().filter { it.id != recordId }
        val jsonArray = JSONArray()
        records.forEach { r ->
            jsonArray.put(recordToJson(r))
        }
        prefs.edit().putString(KEY_RECORDS, jsonArray.toString()).apply()
    }

    /**
     * 清空所有记录
     */
    fun clearAllRecords() {
        prefs.edit().remove(KEY_RECORDS).apply()
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

    private fun recordToJson(record: HealthRecord): JSONObject {
        return JSONObject().apply {
            put("id", record.id)
            put("timestamp", record.timestamp)
            put("type", record.type.name)
            put("value", record.value.toDouble())
            record.secondaryValue?.let { put("secondaryValue", it.toDouble()) }
            put("note", record.note)
        }
    }

    private fun jsonToRecord(json: JSONObject): HealthRecord {
        return HealthRecord(
            id = json.getLong("id"),
            timestamp = json.getLong("timestamp"),
            type = RecordType.valueOf(json.getString("type")),
            value = json.getDouble("value").toFloat(),
            secondaryValue = if (json.has("secondaryValue")) json.getDouble("secondaryValue").toFloat() else null,
            note = json.optString("note", "")
        )
    }
}
