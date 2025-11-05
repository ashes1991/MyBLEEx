package com.ble.kyv.ble

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BLELogManager(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("BLE_LOGS", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_LOGS_PREFIX = "logs_"
        private const val MAX_LOGS_PER_DEVICE = 500 // Максимум 500 подій на пристрій
    }
    
    fun logEvent(event: BLEEvent) {
        val logs = getLogsForDevice(event.deviceMac).toMutableList()
        logs.add(event)
        
        // Обмежуємо кількість логів
        if (logs.size > MAX_LOGS_PER_DEVICE) {
            logs.removeAt(0)
        }
        
        saveLogs(event.deviceMac, logs)
        android.util.Log.d("BLELogManager", "[${event.deviceMac}] ${event.eventType}: ${event.message}")
    }
    
    fun logEvent(deviceMac: String, eventType: BLEEvent.EventType, message: String, details: String? = null) {
        logEvent(BLEEvent(deviceMac, eventType, message, details = details))
    }
    
    fun getLogsForDevice(deviceMac: String): List<BLEEvent> {
        val json = sharedPreferences.getString(KEY_LOGS_PREFIX + deviceMac, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<BLEEvent>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getAllLogs(): Map<String, List<BLEEvent>> {
        val allLogs = mutableMapOf<String, List<BLEEvent>>()
        val allKeys = sharedPreferences.all.keys
        
        allKeys.filter { it.startsWith(KEY_LOGS_PREFIX) }.forEach { key ->
            val mac = key.removePrefix(KEY_LOGS_PREFIX)
            allLogs[mac] = getLogsForDevice(mac)
        }
        
        return allLogs
    }
    
    fun clearLogsForDevice(deviceMac: String) {
        sharedPreferences.edit().remove(KEY_LOGS_PREFIX + deviceMac).apply()
    }
    
    fun clearAllLogs() {
        val allKeys = sharedPreferences.all.keys.filter { it.startsWith(KEY_LOGS_PREFIX) }
        val editor = sharedPreferences.edit()
        allKeys.forEach { editor.remove(it) }
        editor.apply()
    }
    
    fun getLogCount(deviceMac: String): Int {
        return getLogsForDevice(deviceMac).size
    }
    
    private fun saveLogs(deviceMac: String, logs: List<BLEEvent>) {
        val json = gson.toJson(logs)
        sharedPreferences.edit().putString(KEY_LOGS_PREFIX + deviceMac, json).apply()
    }
}

