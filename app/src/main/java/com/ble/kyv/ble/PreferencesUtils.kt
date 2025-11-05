package com.ble.kyv.ble

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class PreferencesUtils(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("BLE_PREFS", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_DEVICES = "devices"
    }
    
    fun saveDevices(devices: List<Device>) {
        val json = gson.toJson(devices)
        sharedPreferences.edit().putString(KEY_DEVICES, json).apply()
    }
    
    fun getDevices(): List<Device> {
        val json = sharedPreferences.getString(KEY_DEVICES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Device>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun addDevice(device: Device) {
        val devices = getDevices().toMutableList()
        // Remove existing device with same MAC if exists
        devices.removeAll { it.mac == device.mac }
        devices.add(device)
        saveDevices(devices)
    }
    
    fun removeDevice(mac: String) {
        val devices = getDevices().toMutableList()
        devices.removeAll { it.mac == mac }
        saveDevices(devices)
    }
    
    fun updateDeviceData(mac: String, firstData: String? = null, secondData: String? = null) {
        val devices = getDevices().toMutableList()
        val device = devices.firstOrNull { it.mac == mac }
        if (device != null) {
            device.firstData = firstData
            device.secondData = secondData
            device.date = Date()
            saveDevices(devices)
        }
    }
    
    fun clearDevices() {
        sharedPreferences.edit().remove(KEY_DEVICES).apply()
    }
}

